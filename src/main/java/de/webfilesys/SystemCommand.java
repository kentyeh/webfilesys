package de.webfilesys;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SystemCommand extends Thread
{
    private static final Logger logger = LogManager.getLogger(SystemCommand.class);
	String prog_name;

	public SystemCommand(String prog_name)
	{
		this.prog_name=prog_name;
	}

        @Override
	public void run()
	{
		Runtime rt=Runtime.getRuntime();

		try
		{
			rt.exec(prog_name);
		}
		catch (Exception e)
		{
			logger.warn(e);
		}
	}
}
