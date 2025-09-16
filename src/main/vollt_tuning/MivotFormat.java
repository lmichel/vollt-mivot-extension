package main.vollt_tuning;

import java.io.IOException;
import java.io.OutputStream;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import tap.formatter.OutputFormat;

/**
 * Connect the VOtable formator that can process annotation with the RESPONSE parameter 
 * equals to "application/x-votable+xml;content=mivot" or "mivot" (nickname). 
 */
public class MivotFormat implements OutputFormat {

	MivotVOTableFormat votFmt;
	public MivotFormat(ServiceConnection service) {
		votFmt=new MivotVOTableFormat(service);
	}
	
	@Override
	public String getMimeType() {
		// TODO Auto-generated method stub
		return "application/x-votable+xml;content=mivot";
	}

	@Override
	public String getShortMimeType() {
		// TODO Auto-generated method stub
		return "mivot";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return votFmt.getDescription();
	}

	@Override
	public String getFileExtension() {
		// TODO Auto-generated method stub
		return votFmt.getFileExtension();
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread)
			throws TAPException, IOException, InterruptedException {
		// TODO Auto-generated method stub
		votFmt.writeResult(result, output, execReport, thread);
	}

}
