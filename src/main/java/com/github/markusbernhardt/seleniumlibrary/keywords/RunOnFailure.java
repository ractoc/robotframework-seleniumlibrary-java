package com.github.markusbernhardt.seleniumlibrary.keywords;

import org.python.util.PythonInterpreter;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.Autowired;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import com.github.markusbernhardt.seleniumlibrary.RunOnFailureKeywordsAdapter;

@RobotKeywords
public class RunOnFailure extends RunOnFailureKeywordsAdapter {

	/**
	 * The keyword to run an failure
	 */
	protected String runOnFailureKeyword = "Capture Page Screenshot";

	/**
	 * Only run keyword on failure if true
	 */
	protected boolean runningOnFailureRoutine;

	/**
	 * Instantiated Logging keyword bean
	 */
	@Autowired
	protected Logging logging;

	// ##############################
	// Keywords
	// ##############################

	@RobotKeyword("Sets the actual and returns the previous keyword to execute when a SeleniumLibrary keyword fails.\r\n" + 
	        "\r\n" + 
	        "The keyword is the name of a keyword (from any available libraries) that will be executed, if a SeleniumLibrary keyword fails. It is not possible to use a keyword that requires arguments. Using the value *Nothing* will disable this feature altogether.\r\n" + 
	        "\r\n" + 
	        "The initial keyword to use is set at importing the library and the keyword that is used by default is `Capture Page Screenshot`. Taking a screenshot when something failed is a very useful feature, but notice that it can slow down the execution.\r\n" + 
	        "\r\n" + 
	        "This keyword returns the name of the previously registered failure keyword. It can be used to restore the original value later.\r\n" + 
	        "\r\n" + 
	        "Example:\r\n" + 
	        " | Register Keyword To Run On Failure | Log Source |  | # Run Log Source on failure. | \r\n" + 
	        " | ${previous kw}= | Register Keyword To Run On Failure | Nothing | # Disable run-on-failure functionality and stors the previous kw name in a variable. | \r\n" + 
	        " | Register Keyword To Run On Failure | ${previous kw} |  | # Restore to the previous keyword. |")
	@ArgumentNames({ "keyword" })
	public String registerKeywordToRunOnFailure(String keyword) {
		String oldKeyword = runOnFailureKeyword;
		String oldKeywordText = oldKeyword != null ? oldKeyword : "No keyword";

		String newKeyword = !keyword.trim().toLowerCase().equals("nothing") ? keyword : null;
		String newKeywordText = newKeyword != null ? newKeyword : "No keyword";

		runOnFailureKeyword = newKeyword;
		logging.info(String.format("%s will be run on failure.", newKeywordText));

		return oldKeywordText;
	}

	// ##############################
	// Internal Methods
	// ##############################

	protected static ThreadLocal<PythonInterpreter> runOnFailurePythonInterpreter = ThreadLocal.withInitial(() -> {
		PythonInterpreter pythonInterpreter = new PythonInterpreter();
		pythonInterpreter.exec("from robot.libraries.BuiltIn import BuiltIn; from robot.running.context import EXECUTION_CONTEXTS; BIN = BuiltIn();");
		return pythonInterpreter;
	});

	public void runOnFailure() {
		if (runOnFailureKeyword == null) {
			return;
		}
		if (runningOnFailureRoutine) {
			return;
		}
		if(runOnFailurePythonInterpreter.get().eval("EXECUTION_CONTEXTS.current").toString().equals("None")) {
			return;
		}
		
		try {
			runOnFailurePythonInterpreter.get().exec(
					String.format("BIN.run_keyword('%s')",
							runOnFailureKeyword.replace("'", "\\'").replace("\n", "\\n")));
		} catch (RuntimeException r) {
			logging.warn(String.format("Keyword '%s' could not be run on failure%s", runOnFailureKeyword,
					r.getMessage() != null ? " '" + r.getMessage() + "'" : ""));
		} finally {
			runningOnFailureRoutine = false;
		}
	}

}
