package strategies;

import collaboration.Task;
import collaboration.TaskInternals;
import collaboration.WorkUnit;

public class GreedyAssignmentTask extends EmergenceStrategy {
	
	public void increment(Task task, 
			TaskInternals singleTaskInternal, int n, double experience){
		WorkUnit workDone = singleTaskInternal.getWorkDone();
		workDone.increment(n * experience);
		doAftearmath(task, singleTaskInternal);
	}
	
	@Override
	protected void doAftearmath(Task task, TaskInternals singleTaskInternal) {
		if (singleTaskInternal.isWorkDone()){
			super.say("Work in taskInternal:" + singleTaskInternal + " is done.");
			task.removeSkill(singleTaskInternal.getSkill().getName());
		}
	}

}