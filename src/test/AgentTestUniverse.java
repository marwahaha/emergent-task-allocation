package test;

import java.util.ArrayList;

import collaboration.Agent;
import collaboration.AgentInternals;
import collaboration.CollaborationBuilder;
import collaboration.Experience;
import collaboration.Skill;
import collaboration.Skills;

public class AgentTestUniverse {

	public static ArrayList<Agent> DATASET = new ArrayList<Agent>();

	public static void init() {
		DATASET.clear();
		
		Agent agent1 = new Agent("Joe", "Smith", "Java newbie-1");
		System.out.println("Initializing agent 1..");
		Skill skill11 = ((Skills) CollaborationBuilder.skills).getSkill("Java");
		Experience experience11 = new Experience(3, 18);
		Skill skill12 = ((Skills) CollaborationBuilder.skills).getSkill("C");
		Experience experience12 = new Experience(1, 10);
		Skill skill13 = ((Skills) CollaborationBuilder.skills).getSkill("XML");
		Experience experience13 = new Experience(0, 10);
		agent1.addSkill("Java", new AgentInternals(skill11, experience11));
		agent1.addSkill("C", new AgentInternals(skill12, experience12));
		agent1.addSkill("XML", new AgentInternals(skill13, experience13));
		
		Agent agent2 = new Agent("John", "Dereck", "C expert-1");
		System.out.println("Initializing agent 2..");
		Skill skill21 = ((Skills) CollaborationBuilder.skills).getSkill("Java");
		Experience experience21 = new Experience(2, 18);
		Skill skill22 = ((Skills) CollaborationBuilder.skills).getSkill("C");
		Experience experience22 = new Experience(9, 18);
		Skill skill23 = ((Skills) CollaborationBuilder.skills).getSkill("XML");
		Experience experience23 = new Experience(1, 10);
		agent2.addSkill("Java", new AgentInternals(skill21, experience21));
		agent2.addSkill("C", new AgentInternals(skill22, experience22));
		agent2.addSkill("XML", new AgentInternals(skill23, experience23));
		
		Agent agent3 = new Agent("Adam", "Smith", "Newbie");
		System.out.println("Initializing agent 3..");
		Skill skill31 = ((Skills) CollaborationBuilder.skills).getSkill("Java");
		Experience experience31 = new Experience(0, 10);
		Skill skill32 = ((Skills) CollaborationBuilder.skills).getSkill("C");
		Experience experience32 = new Experience(0, 10);
		Skill skill33 = ((Skills) CollaborationBuilder.skills).getSkill("XML");
		Experience experience33 = new Experience(1, 20);
		agent3.addSkill("Java", new AgentInternals(skill31, experience31));
		agent3.addSkill("C", new AgentInternals(skill32, experience32));
		agent3.addSkill("XML", new AgentInternals(skill33, experience33));
		
		DATASET.add(agent1);
		DATASET.add(agent2);
		DATASET.add(agent3);
	}

	
}
