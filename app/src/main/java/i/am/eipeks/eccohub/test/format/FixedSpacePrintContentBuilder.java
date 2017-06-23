package i.am.eipeks.eccohub.test.format;

import com.laiqian.print.model.PrintContent;
import com.laiqian.print.util.PrintUtils;

public class FixedSpacePrintContentBuilder extends BaseFormatedBuilder {
	
	public FixedSpacePrintContentBuilder() {
		super();
		setDefaults();
	}
	
	public FixedSpacePrintContentBuilder(PrintContent.Builder builder) {
		super(builder);
		setDefaults();
	}
	
	private void setDefaults() {
		int paperLength = PrintUtils.getWidthLength(getWidth());
		int first;
		int second;
		int third;
		int fourth;
		
		first = paperLength * 3 / 4;
		second = paperLength - first;
		changeSetting(new int[] {first, second});
		
		first = paperLength / 2;
		second = paperLength / 4;
		third = paperLength - first - second;
		changeSetting(new int[] {first, second, third});
		
		first = paperLength / 3;
		second = paperLength / 5;
		third = second;
		fourth = paperLength - first - second - third;
		changeSetting(new int[] {first, second, third, fourth});
	}


	/**
	 *
	 * @param strings
	 */
	@Override
	public void appendStrings(String[] strings) {
		int length = strings.length;
		PrintUtils.appendFixedSpaceString(getBuilder(), getSetting(length), getAlign(length), strings, getSize());
	}
	
}
