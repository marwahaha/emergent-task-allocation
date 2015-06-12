package collaboration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import logger.VerboseLogger;

import org.apache.commons.lang3.SystemUtils;

import au.com.bytecode.opencsv.CSVReader;

/***
 * Here are all skills known to GitHub read and hold in ArrayList for more info,
 * please visit:
 * https://github.com/github/linguist/blob/master/lib/linguist/languages.yml 
 * In other words, this class loads all know programming languages,
 * and it includes deleted language definitions as well (compability backwards)
 * 
 * @author Oskar Jarczyk
 * @since 1.0
 * @version 2.0.11
 */
public class SkillFactory {

	/**
	 * File format:
	 * 
	 * language1,type\r\n 
	 * language2,type\r\n 
	 * ... 
	 * langauage{i},type
	 * 
	 * around 305 entries
	 */
	private static String filename = SystemUtils.IS_OS_LINUX ? "data/all-languages.csv"
			: "data\\all-languages.csv";
	public static ArrayList<Skill> skills = new ArrayList<Skill>();

	private static SkillFactory instance = null;

	private SkillFactory() {
		say("[SkillFactory] object created");
	}

	public static SkillFactory getInstance() {
		if (instance == null) {
			instance = new SkillFactory();
		}
		return instance;
	}

	public Skill getSkill(String name) {
		for (Skill skill : skills) {
			if (skill.getName().toLowerCase().equals(name.toLowerCase())) {
				return skill;
			}
		}
		return null;
	}

/*	public Skill getRandomSkill() {
		if (SimulationParameters.skillFactoryRandomMethod
				.equals("normal_distribution")) {
			return getRandomSkill(RandomMethod.NORMAL_DISTRIBUTION);
		} else if (SimulationParameters.skillFactoryRandomMethod
				.equals("breit_wigner")) {
			return getRandomSkill(RandomMethod.BREIT_WIGNER);
		} else if (SimulationParameters.skillFactoryRandomMethod
				.equals("normal_distribution")) {
			return getRandomSkill(RandomMethod.POISSON_DISTRIBUTION);
		}
		return getRandomSkill(RandomMethod.RANDOM_GENERATOR);
	}*/

/*	public Skill getRandomSkill(RandomMethod method) {
		double randomized;
		switch (method) {
		case POISSON_DISTRIBUTION:
			Poisson poisson = new Poisson(0.0,
					cern.jet.random.Poisson.makeDefaultGenerator());
			randomized = poisson.nextDouble();
			assert (randomized >= 0.) && (randomized <= 1.);
			return skills.get((int) (randomized * skills.size()));
		case RANDOM_GENERATOR:
			int i = RandomHelper.nextIntFromTo(0, skills.size() - 1);
			return skills.get(i);
		case NORMAL_DISTRIBUTION:
			Normal normal = new Normal(0.0, 1.0,
					cern.jet.random.Normal.makeDefaultGenerator());
			randomized = normal.nextDouble();
			assert (randomized >= 0.) && (randomized <= 1.);
			return skills.get((int) (randomized * skills.size()));
		case BREIT_WIGNER:
			BreitWigner bw = new BreitWigner(1.0, 1.0, 1.0,
					cern.jet.random.BreitWigner.makeDefaultGenerator());
			randomized = bw.nextDouble();
			assert (randomized >= 0.) && (randomized <= 1.);
			return skills.get((int) (randomized * skills.size()));
		default:
			break;
		}
		return null;
	}*/

	public void buildSkillsLibrary(boolean verbose) throws IOException, FileNotFoundException {
		say("Searching for [file] in path: " + new File(".").getAbsolutePath());
		CSVReader reader = new CSVReader(new FileReader(filename));
		String[] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			Skill skill = new Skill(nextLine[0], nextLine[1], skills.size() + 1);
			skills.add(skill);
			if (verbose)
				say("[Skill] " + skill.getId() + ": " + skill.getName()
					+ " added to [Skill Factory]");
		}
		reader.close();
	}

	private static void say(String s) {
		VerboseLogger.say(s);
	}

}
