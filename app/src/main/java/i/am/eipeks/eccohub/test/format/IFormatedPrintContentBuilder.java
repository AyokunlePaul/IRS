package i.am.eipeks.eccohub.test.format;

import com.laiqian.print.model.PrintContent;

public interface IFormatedPrintContentBuilder {
	public PrintContent build();
	public void appendTitle(String title);
	public void appendString(String str);
	public void appendStrings(String[] strings);
}
