package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2003 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.*;

import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.NSMutableArray;
import com.apple.cocoa.foundation.NSNotification;
import com.apple.cocoa.foundation.NSSelector;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class CDDownloadController {
	private static Logger log = Logger.getLogger(CDDownloadController.class);

	private NSWindow window;

	public void setWindow(NSWindow window) {
		this.window = window;
		this.window.setDelegate(this);
	}

	public NSWindow window() {
		return this.window;
	}

	private NSTextField urlField;

	public void setUrlField(NSTextField urlField) {
		this.urlField = urlField;
	}

	private static NSMutableArray instances = new NSMutableArray();

	public CDDownloadController() {
		instances.addObject(this);
		if (false == NSApplication.loadNibNamed("Download", this)) {
			log.fatal("Couldn't load Download.nib");
		}
	}

	public void awakeFromNib() {
		log.debug("awakeFromNib");
		CDQueueController.instance().window().makeKeyAndOrderFront(null);
		NSApplication.sharedApplication().beginSheet(
		    this.window, //sheet
		    CDQueueController.instance().window(),
		    this, //modalDelegate
		    new NSSelector(
		        "downloadSheetDidEnd",
		        new Class[]{NSWindow.class, int.class, Object.class}
		    ), // did end selector
		    null); //contextInfo
	}

	public void windowWillClose(NSNotification notification) {
		instances.removeObject(this);
	}

	public void closeSheet(Object sender) {
		// Ends a document modal session by specifying the sheet window, sheet. Also passes along a returnCode to the delegate.
		NSApplication.sharedApplication().endSheet(this.window, ((NSButton) sender).tag());
	}

	public void downloadSheetDidEnd(NSWindow sheet, int returncode, Object context) {
		log.debug("loginSheetDidEnd");
		this.window.orderOut(null);
		switch (returncode) {
			case (NSAlertPanel.DefaultReturn):
				try {
					URL url = new URL(urlField.stringValue());
					Host host = new Host(url.getProtocol(), url.getHost(), url.getPort(), new Login(url.getUserInfo()));
					Session session = SessionFactory.createSession(host);
					Path path = null;
					String file = url.getFile();
					if (file.length() > 1) {
						path = PathFactory.createPath(SessionFactory.createSession(host), file);
						this.window.orderOut(null);
						CDQueueController.instance().addItem(new Queue(path,
						    Queue.KIND_DOWNLOAD), true);
					}
					else {
						throw new MalformedURLException("URL must contain reference to a file");
					}
				}
				catch (MalformedURLException e) {
					NSAlertPanel.beginCriticalAlertSheet(
					    "Error", //title
					    "OK", // defaultbutton
					    null, //alternative button
					    null, //other button
					    this.window, //docWindow
					    null, //modalDelegate
					    null, //didEndSelector
					    null, // dismiss selector
					    null, // context
					    e.getMessage() // message
					);

				}
				break;
			case (NSAlertPanel.AlternateReturn):
				break;
		}
	}
}