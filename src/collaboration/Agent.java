package collaboration;

import github.AgentModeling;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import load.FunctionSet;
import load.GranularityOption;
import repast.simphony.annotate.AgentAnnot;
import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NodeCreator;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.ui.probe.ProbeID;
import repast.simphony.util.ContextUtils;
import strategies.Strategy;
import strategies.Strategy.SkillChoice;
import strategies.Strategy.TaskChoice;
import tasks.CentralAssignmentOrders;
import argonauts.GranulatedChoice;
import argonauts.PersistJobDone;
import argonauts.PersistRewiring;
import collaboration.Utility.UtilityType;

/***
 * Simulation agent - a GitHub programmer as restored from data on users
 * activity with a help of aggregators like BrainJar etc.
 * 
 * @author Oskar Jarczyk
 * @since 1.0
 * @version 2.0.11
 */
@AgentAnnot(displayName = "Agent")
public class Agent implements NodeCreator<Agent> {

	/**
	 * This value is used to automatically generate agent identifiers.
	 * 
	 * 42 - Answer to life the universe and everything, even GitHub
	 * 
	 * @field serialVersionUID
	 */
	public static final long serialVersionUID = 42L;
	private static GameController gameController;
	public static int totalAgents = 0;

	private AgentSkills agentSkills;
	private Strategy strategy;

	private final int id = ++totalAgents;
	private String firstName;
	private String lastName;
	private String nick;

	private CentralAssignmentOrders centralAssignmentOrders;

	public Agent() {
		this("Undefined name", "Undefined", "Agent_");
	}

	public Agent(String firstName, String lastName, String nick) {
		this.agentSkills = new AgentSkills();
		System.out.println("[Agent] constructor called");
		AgentModeling.fillWithSkills(this);
		this.firstName = firstName;
		this.lastName = lastName;
		this.nick = nick + this.id;
		this.agentSkills.backup();
	}

	@SuppressWarnings("unchecked")
	public GameController initGameController() {
		Context<Agent> context = ContextUtils.getContext(this);
		Context<Object> parentContext = ContextUtils.getParentContext(context);
		gameController = (GameController) parentContext.getObjects(
				GameController.class).get(0);
		return gameController;
	}

	public Double getUtility() {
		if (FunctionSet.INSTANCE.getChosen().equals(UtilityType.NormalizedSum)) {
			return getNormalizedSumUtility();
		} else if (FunctionSet.INSTANCE.getChosen().equals(UtilityType.MaxSkill)) {
			return getBestSkillUtility();
		} else {
			return getWorstSkillUtility();
		}
	}

	public Double getNormalizedSumUtility() {
		return Utility.getNormalizedSum(getAgentInternals());
	}
	
	public Double getBestSkillUtility() {
		return Utility.getBestSkill(getAgentInternals());
	}
	
	public Double getWorstSkillUtility() {
		return Utility.getWorstSkill(getAgentInternals());
	}

	public String getDecimalFormatUtility() {
		return new DecimalFormat("#.######").format(getUtility());
	}

	public void mutate() {
		// 1% chances for deleting (abandoning) skill
		if (RandomHelper.nextDoubleFromTo(0, 1) <= 0.01) {
			Object[] allSkills = agentSkills.getSkills().keySet().toArray();
			agentSkills.removeSkill((String) allSkills[RandomHelper
					.nextIntFromTo(0, allSkills.length - 1)]);
		}
	}

	public void addSkill(String key, AgentInternals agentInternals) {
		getCurrentSkills().put(key, agentInternals);
	}

	/**
	 * Actually used very rarely, as far as I know - only when experience after
	 * decayExp operation returns 0 Assertion that agent possess this skill
	 * before removal
	 * 
	 * @param key
	 *            - name of the Skill to remove
	 */
	public void removeSkill(String key, boolean skipAssertion) {
		assert skipAssertion ? true : getCurrentSkills().containsKey(key);
		getCurrentSkills().remove(key);
	}

	public void removeSkill(Skill key, boolean skipAssertion) {
		removeSkill(key.getName(), skipAssertion);
	}

	public Collection<AgentInternals> getAgentInternals() {
		return getCurrentSkills().values();
	}

	public AgentInternals getAgentInternals(String key) {
		return getCurrentSkills().get(key);
	}

	public AgentInternals getAgentInternalsOrCreate(String key) {
		AgentInternals result = null;
		if (getCurrentSkills().get(key) == null) {
			result = (new AgentInternals(((Skills) CollaborationBuilder.skills).getSkill(
					key), new Experience(true)));
			getCurrentSkills().put(key, result);
			result = getCurrentSkills().get(key);
		} else {
			result = getCurrentSkills().get(key);
		}
		return result;
	}

	public Collection<Skill> getSkills() {
		ArrayList<Skill> skillCollection = new ArrayList<Skill>();
		Collection<AgentInternals> internals = this.getAgentInternals();
		for (AgentInternals ai : internals) {
			skillCollection.add(ai.getSkill());
		}
		return skillCollection;
	}

	public void resetMe() {
		getAgentSkills().reset();
	}

	public int getId() {
		return this.id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 100)
	public void step() {
		/*System.out.println("Step(" + getTick() + ") of Agent " + this.id
				+ " scheduled method launched.");*/

		if (GranularityOption.INSTANCE.getChosen()) {
			GranulatedChoice granulated = PersistRewiring
					.getGranulatedChoice(this);

			if (granulated != null) {
				// randomize decision
				double leavingCurrentChance = RandomHelper.nextDoubleFromTo(0,
						100);
				if (leavingCurrentChance <= 75) {
					/*System.out.println("Step(" + getTick() + ") of Agent " + this.id
							+ " continuuing granularity");*/
					// continue work on the same skill
					// but check if the is any work left in this particular task
					Boolean workDone = granulated.getTaskChosen()
							.workOnTaskFromContinuum(this, granulated,
									this.strategy.skillChoice);
					if (!workDone) {
						// chose new task for granulated choice !
						Task taskToWork = Tasks.chooseTask(this,
								this.strategy.getTaskChoice());
						executeJob(taskToWork);
						if (taskToWork != null) {
							PersistRewiring.setOccupation(this, taskToWork);
						}
					}
					// EnvironmentEquilibrium.setActivity(true);
				} else {
					System.out.println("Step(" + getTick() + ") of Agent " + this.id
							+ " choosing new task for granulated choice");
					// chose new task for granulated choice !
					Task taskToWork = Tasks.chooseTask(this,
							this.strategy.getTaskChoice());
					executeJob(taskToWork);
					if (taskToWork != null) {
						PersistRewiring.setOccupation(this, taskToWork);
					}
				}
			} else {
				System.out.println("Step("
						+ getTick()
						+ ") of Agent "
						+ this.id
						+ " first run, chose new task and assign granulated choice");
				// first run
				// chose new task and assign granulated choice !
				Task taskToWork = Tasks.chooseTask(this,
						this.strategy.getTaskChoice());
				executeJob(taskToWork);
				if (taskToWork != null) {
					PersistRewiring.setOccupation(this, taskToWork);
				}
			}
			/*****************************
			 * Granularity ends here
			 *****************************/
		} else { // block without granularity
			// [Agent] Aj uses Aj_S ([Strategy] for choosing tasks)
			// and chooses a Task Ti to work on
			Task taskToWork = Tasks.chooseTask(this,
					this.strategy.getTaskChoice());
			executeJob(taskToWork);
		}
	}

	private void executeJob(Task taskToWork) {
		// This agent will work on task Task taskToWork
		if ((taskToWork != null) && (taskToWork.getTaskInternals().size() > 0)) {

			assert taskToWork.getTaskInternals().size() > 0;
			//System.out.println("Agent " + this.id + " will work on task " + taskToWork.getId());
			if ((this.getCentralAssignmentOrders() != null)
					&& (this.getCentralAssignmentOrders()
							.getChosenTask()
							.getTaskInternals(
									this.getCentralAssignmentOrders()
											.getChosenSkillName()) != null)) {
				taskToWork.workOnTaskCentrallyControlled(this);
				EnvironmentEquilibrium.setActivity(true);
			} else {
				taskToWork.workOnTask(this, this.strategy.skillChoice);
				EnvironmentEquilibrium.setActivity(true);
			}
		} else {

			if (Tasks.stillNonEmptyTasks()) {
				Task randomTaskToWork = Tasks.chooseTask(this,
						Strategy.TaskChoice.RANDOM);
				assert randomTaskToWork.getTaskInternals().size() > 0;
				/*System.out.println("Agent " + this.id + " will work on task "
						+ randomTaskToWork.getId());*/
				if ((this.getCentralAssignmentOrders() != null)
						&& (this.getCentralAssignmentOrders()
								.getChosenTask()
								.getTaskInternals(
										this.getCentralAssignmentOrders()
												.getChosenSkillName()) != null)) {
					randomTaskToWork.workOnTaskCentrallyControlled(this);
				} else
					randomTaskToWork.workOnTask(this, SkillChoice.RANDOM);
				EnvironmentEquilibrium.setActivity(true);
			}
		}

	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		System.out.println("Agent's login set to: " + nick);
		this.nick = nick;
	}

	public String getName() {
		return this.toString() + " (" + this.firstName + " " + this.lastName;
	}

	public GameController getGameController() {
		return gameController == null ? initGameController() : gameController;
	}

	public int getIteration() {
		return getGameController().getCurrentIteration() + 1;
	}

	public int getGeneration() {
		return getGameController().getCurrentGeneration() + 1;
	}

	private double getTick() {
		return getGameController().getCurrentTick();
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public Strategy.SkillChoice getSkillStrategy() {
		return strategy.skillChoice;
	}

	public Strategy.TaskChoice getTaskStrategy() {
		return strategy.getTaskChoice();
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}

	public CentralAssignmentOrders getCentralAssignmentOrders() {
		return centralAssignmentOrders;
	}

	public void setCentralAssignmentOrders(
			CentralAssignmentOrders centralAssignmentOrders) {
		if (centralAssignmentOrders != null) {
			System.out.println("Agent " + this.nick + " got an order to work on "
					+ centralAssignmentOrders);
		}
		this.centralAssignmentOrders = centralAssignmentOrders;
	}

	public String describeExperience() {
		Collection<AgentInternals> internals = this.getAgentInternals();
		Map<String, String> deltaE = new HashMap<String, String>();
		for (AgentInternals ai : internals) {
			deltaE.put(ai.getSkill().getName(), (new DecimalFormat("#.######"))
					.format(ai.getExperience().getDelta()));
		}
		return deltaE.entrySet().toString();
	}

	public String getDecimalFormatGeneralExperience() {
		return new DecimalFormat("#.######").format(getGeneralExperience());
	}
	
	public Double getGeneralExperience() {
		Collection<AgentInternals> internals = this.getAgentInternals();
		double sum = 0.0;
		for (AgentInternals ai : internals) {
			sum += ai.getExperience().getDelta();
		}
		return (sum / internals.size());
	}

	public Double getFilteredExperience(Collection<Skill> common) {
		Collection<AgentInternals> internals = this.getAgentInternals();
		double sum = 0.0;
		int count = 0;
		for (AgentInternals ai : internals) {
			if (common.contains(ai.getSkill())) {
				count++;
				sum += ai.getExperience().getDelta();
			}
		}
		return sum / count;
	}

	public Double getExperience(Skill skill) {
		return this.describeExperience(skill, false, false);
	}

	/***
	 * Returns the experience of Agent in a particular programming Skill
	 * 
	 * @param skill
	 * @param unknownSkillIsZero
	 *            - if true, returns 0.0 instead of null, when Skill not found
	 *            in Agent's set of skills
	 * @param forceCreate
	 *            - if Agent don't possess this skill, but argument is set as
	 *            true, he will now have it, use with caution and reason
	 * @return Double - agent's experience in given skill
	 */
	public Double describeExperience(Skill skill, Boolean unknownSkillIsZero,
			Boolean forceCreate) {
		if (getCurrentSkills().get(skill.getName()) == null) {
			if (forceCreate) {
				AgentInternals result = (new AgentInternals(((Skills) CollaborationBuilder.skills).getSkill(skill.getName()),
						new Experience(true)));
				getCurrentSkills().put(skill.getName(), result);
			} else {
				if (unknownSkillIsZero) {
					return 0d;
				} else {
					return null;
				}
			}
		}
		return getCurrentSkills().get(skill.getName()).getExperience()
				.getDelta();
	}

	private Map<String, AgentInternals> getCurrentSkills() {
		return agentSkills.getSkills();
	}

	public AgentSkills getAgentSkills() {
		return agentSkills;
	}

	public void setAgentSkills(AgentSkills agentSkills) {
		this.agentSkills = agentSkills;
	}

	public int usesHomophyly() {
		return this.strategy.getTaskChoice().equals(TaskChoice.HOMOPHYLY) ? 1 : 0;
	}

	public int usesHeterophyly() {
		return this.strategy.getTaskChoice().equals(TaskChoice.HETEROPHYLY) ? 1 : 0;
	}

	public int usesPreferential() {
		return this.strategy.getTaskChoice().equals(TaskChoice.PREFERENTIAL) ? 1
				: 0;
	}

	@ProbeID()
	@Override
	public String toString() {
		return getNick();
	}

	@Override
	public int hashCode() {
		return nick.hashCode() * id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Agent)) {
			return false;
		}
		if ((this.id == ((Agent) obj).id)
				&& (this.nick.toLowerCase().equals((((Agent) obj).nick
						.toLowerCase()))))
			return true;
		else
			return false;
	}

	@Override
	public Agent createNode(String label) {
		return createNode("Agent-" + getNick());
	}

	public boolean wasWorkingOnAnything() {
		return PersistJobDone.getJobDone().containsKey(this.getNick());
	}
}

class EnvironmentEquilibrium {

	private static boolean activity = false;

	public static synchronized boolean getActivity() {
		return activity;
	}

	public static synchronized void setActivity(boolean defineActivity) {
		activity = defineActivity;
	}

}