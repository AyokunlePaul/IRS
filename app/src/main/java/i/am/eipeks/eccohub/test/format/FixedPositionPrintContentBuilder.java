package i.am.eipeks.eccohub.test.format;

import com.laiqian.print.model.PrintContent;
import com.laiqian.print.util.PrintUtils;

public class FixedPositionPrintContentBuilder extends BaseFormatedBuilder {
	private int width = 58;
	
	public FixedPositionPrintContentBuilder() {
		super();
		setDefaults();
	}
	
	public FixedPositionPrintContentBuilder(PrintContent.Builder builder) {
		super(builder);
		setDefaults();
	}
	
	private void setDefaults() {
		int paperLength = PrintUtils.getWidthLength(getSize());
		int slice;
		changeSetting(new int[] {0});
		changeSetting(new int[] {0, paperLength - 1});
		slice = paperLength / 3;
		changeSetting(new int[] {0, slice * 2, paperLength - 1});
		slice = paperLength / 4;
		changeSetting(new int[] {0, slice * 2, slice * 3, paperLength - 1});
	}
	
	@Override
	public void appendStrings(String[] strings) {
		int length = strings.length;
		PrintUtils.appendFixedPositionString(getBuilder(), width, getSetting(length), getAlign(length), strings,
				getSize());
	}
	
}
