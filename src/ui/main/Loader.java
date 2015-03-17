package ui.main;

import javax.swing.SwingWorker;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class Loader extends SwingWorker<LoaderData, ClassInfo> {
	private LoaderData data;
	private double progress, increment;
	private String packageName;
	
	public Loader(String packageName) {
		this.packageName = packageName;
		data = new LoaderData();
	}
	
	protected LoaderData doInBackground() throws Exception {
		setProgress(0);
		ImmutableSet<ClassInfo> set = ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClassesRecursive(packageName);
		progress = 25.0;
		setProgress((int) progress);
		increment = (100.0 - progress) / set.size();
		for(ClassInfo classInfo : set) {
			data.addData(classInfo.load());
			setProgress((int) (progress += increment));
			// forcing the progress bar to update
			Thread.sleep(0, 1);
		}
		return data;
	}
}
