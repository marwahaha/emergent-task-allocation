package collaboration;

import github.DataSet;
import intelligence.AgentComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logger.PjiitOutputter;
import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.random.RandomHelper;
import strategies.Strategy;
import strategies.StrategyDistribution;
import test.AgentTestUniverse;
import utils.LaunchStatistics;
import utils.NamesGenerator;

/***
 * Agents context, hence the contex.xml where Repast Simphony holds the context
 * structure. Context holds all simulation Agents.
 * 
 * @author Oskar Jarczyk
 * @version 2.0.6
 */
public class Agents extends DefaultContext<Agent> {

	/**
	 * This value is used to automatically generate agent identifiers.
	 * 
	 * Schwarzschild radius of Milky Way is 2.08*1015 (~0.2 ly)
	 * 
	 * @field serialVersionUID
	 */
	public static final long serialVersionUID = 2081015L;

	private List<Agent> listAgents;
	private DataSet dataSet;
	private StrategyDistribution strategyDistribution;
	private LaunchStatistics launchStatistics;
	private Integer allowedLoad;

	public Agents(DataSet dataSet, StrategyDistribution strategyDistribution,
			LaunchStatistics launchStatistics, Integer allowedLoad) {
		super("Agents");

		this.dataSet = dataSet;
		this.strategyDistribution = strategyDistribution;
		this.launchStatistics = launchStatistics;
		this.allowedLoad = allowedLoad;

		initializeAgents(this);
	}

	public Agents(StrategyDistribution strategyDistribution, Integer allowedLoad) {
		this(DataSet.getInstance(), strategyDistribution, 
				LaunchStatistics.getInstance(), allowedLoad);
	}

	private void addAgents(Context<Agent> context) {
		Integer agentCnt = SimulationParameters.multipleAgentSets ? allowedLoad
				: SimulationParameters.agentCount;

		listAgents = NamesGenerator.getnames(agentCnt);
		for (int i = 0; i < agentCnt; i++) {
			Agent agent = listAgents.get(i);

			Strategy strategy = strategyDistribution.isMultiple() ? Strategy
					.getInstance(strategyDistribution, i, agentCnt)
					: new Strategy(strategyDistribution.getTaskStrategy(),
							strategyDistribution.getSkillStrategy());
			say("[Strategy] prepared for agent is: " + strategy.toString());

			agent.setStrategy(strategy);
			say(agent.toString());
			say("In add [agent] i: " + i);
			// Required adding agent to context

			for (AgentInternals ai : agent.getAgentInternals()) {
				assert ai.getExperience().getValue() > 0;
				say("For a=" + agent.toString() + " delta is "
						+ ai.getExperience().getDelta());
				say("For a=" + agent.toString() + " value is "
						+ ai.getExperience().getValue());
				say("For a=" + agent.toString() + " top is "
						+ ai.getExperience().getTop());
			}
			context.add(agent);
		}
		launchStatistics.agentCount = agentCnt;
	}

	private void initializeAgents(Context<Agent> context) {
		if (dataSet.isContinuus()) {
			// TODO: make brainjar
			addAgents(context);
		} else if (dataSet.isMockup()) {
			addAgents(context);
		} else if (dataSet.isTest()) {
			listAgents = new ArrayList<Agent>();
			AgentTestUniverse.init();
			initializeValidationAgents(context);
		} else {
			assert false;
		}
	}

	private void initializeValidationAgents(Context<Agent> context) {
		for (Agent agent : AgentTestUniverse.DATASET) {
			say("Adding validation agent to pool..");
			Strategy strategy = new Strategy(
					strategyDistribution.getTaskStrategy(),
					strategyDistribution.getSkillStrategy());
			agent.setStrategy(strategy);
			listAgents.add(agent);
			say(agent.toString() + " added to pool.");

			// Required adding agent to context
			context.add(agent);
			launchStatistics.agentCount++;
		}
	}

	private void say(String s) {
		PjiitOutputter.say(s);
	}

	/***
	 * Evolution with Stochasting Universal Sampling (SUS)
	 * 
	 * @author Paulina Adamska
	 * @since 2.0, partially 1.3
	 * @version 2.0.6
	 * @param agents
	 *            - list of agents to take part in evolution
	 */
	public static void stochasticSampling(ArrayList<Agent> population) {
		if (population.size() == 0)
			return;
		Collections.sort(population, new AgentComparator());
		double min = population.get(population.size() - 1).getUtility();
		double scaling = min < 0 ? ((-1) * min) : 0;
		double maxRange = 0;
		ArrayList<Double> ranges = new ArrayList<Double>();
		ArrayList<Strategy> strategiesBackup = new ArrayList<Strategy>();

		for (Agent p : population) {
			maxRange += (p.getUtility() + scaling);
			ranges.add(maxRange);
			strategiesBackup.add(p.getStrategy().copy());
		}

		double step = maxRange / population.size();
		double start = RandomHelper.nextDoubleFromTo(0, 1) * step;
		for (int i = 0; i < population.size(); i++) {
			int selectedPlayer = population.size() - 1;
			for (int j = 0; j < ranges.size(); j++) {
				double pointer = start + i * step;
				if (pointer < ranges.get(j)) {
					selectedPlayer = j;
					break;
				}
			}
			Agent nextAgent = population.get(i);
			nextAgent.getStrategy().copyStrategy(
					strategiesBackup.get(selectedPlayer));

			nextAgent.mutate();
		}
	}

}
