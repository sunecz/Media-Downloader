package sune.app.mediadown.exception;

import java.util.Objects;

import sune.app.mediadown.report.ReportContext;

/** @since 00.02.09 */
public class WrappedReportContextException extends Exception {
	
	private static final long serialVersionUID = -7119683221000939321L;
	
	private final ReportContext context;
	
	public WrappedReportContextException(Throwable cause, ReportContext context) {
		super(cause);
		this.context = Objects.requireNonNull(context);
	}
	
	public ReportContext context() {
		return context;
	}
}