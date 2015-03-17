package ui.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;

public class PrintUtils implements Printable, Pageable {
	private static final double MIN_SCALE = 0.5;
	private static final PrintRequestAttributeSet printRequestAttributes;
	private Component component;
	private Dimension size;
	private PageFormat pageFormat;
	private boolean rotated;
	private int numberOfPages;
	private double scale;
	
	static {
		printRequestAttributes = new HashPrintRequestAttributeSet();
		printRequestAttributes.add(MediaSizeName.NA_LETTER);
	}
	
	public static synchronized void printComponent(Component component) throws PrinterException {
		if(component == null) {
			return;
		}
		new PrintUtils(component).print();
	}
	
	private PrintUtils(Component component) {
		this.component = component;
		Dimension prefSize = component.getPreferredSize();
		Dimension curSize = component.getSize();
		size = curSize.height > prefSize.height && curSize.width > prefSize.width ? curSize : prefSize;
		updatePageFormat(null);
	}
	
	public void print() throws PrinterException {
		PrinterJob printerJob = PrinterJob.getPrinterJob();
		printerJob.setPageable(this);
		printerJob.setCopies(1);
		if(printerJob.printDialog()) {
			updatePageFormat(printerJob);
			printerJob.print();
		}
	}
	
	public int getNumberOfPages() {
		return numberOfPages;
	}
	
	public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
		if(pageIndex >= numberOfPages) {
			throw new IndexOutOfBoundsException();
        }
		return pageFormat;
	}
	
	public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
		if(pageIndex >= numberOfPages) {
			throw new IndexOutOfBoundsException();
        }
		return this;
	}
	
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		if(pageIndex >= numberOfPages) {
            return NO_SUCH_PAGE;
        }
		int x = 0;
		int y = 0;
		for(int i = 0; i < pageIndex; ++i) {
			if(rotated) {
				++x;
				if(x * (pageFormat.getImageableWidth() / scale) >= size.width) {
					x = 0;
					++y;
				}
			}
			else {
				++y;
				if(y * (pageFormat.getImageableHeight() / scale) >= size.height) {
					y = 0;
					++x;
				}
			}
		}
		Graphics2D g = (Graphics2D) graphics;
		g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		g.translate(-x * pageFormat.getImageableWidth(), -y * pageFormat.getImageableHeight());
		g.scale(scale, scale);
		component.paint(g);
		return PAGE_EXISTS;
	}
	
	private void updatePageFormat(PrinterJob printerJob) {
		if(printerJob == null) {
			printerJob = PrinterJob.getPrinterJob();
		}
		Object obj = printerJob.getPrintService().getSupportedAttributeValues(MediaPrintableArea.class, null, printRequestAttributes);
		if(obj != null) {
			if(obj instanceof MediaPrintableArea[]) {
				printRequestAttributes.add(((MediaPrintableArea[]) obj)[0]);
			}
			else {
				printRequestAttributes.add((MediaPrintableArea) obj);
			}
		}
		pageFormat = printerJob.getPageFormat(printRequestAttributes);
		rotated = size.width > size.height && pageFormat.getImageableWidth() < pageFormat.getImageableHeight();
		if(rotated) {
			pageFormat.setOrientation(PageFormat.REVERSE_LANDSCAPE);
		}
		scale = Math.max(Math.min(pageFormat.getImageableHeight() / size.height, pageFormat.getImageableWidth() / size.width), MIN_SCALE);
		numberOfPages = ((int) Math.ceil((size.width * scale) / pageFormat.getImageableWidth())) * ((int) Math.ceil((size.height * scale) / pageFormat.getImageableHeight()));
	}
}
