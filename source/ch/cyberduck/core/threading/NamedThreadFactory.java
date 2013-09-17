package ch.cyberduck.core.threading;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.ch
 */

import org.apache.log4j.Logger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @version $Id$
 */
public class NamedThreadFactory implements ThreadFactory {
    private static final Logger log = Logger.getLogger(NamedThreadFactory.class);

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private String name;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(final Runnable action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Create thread for runnable %s", action));
        }
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final ActionOperationBatcher autorelease = ActionOperationBatcherFactory.get();
                try {
                    action.run();
                }
                finally {
                    autorelease.operate();
                }
            }
        });
        thread.setDaemon(true);
        thread.setName(String.format("%s-%d", name, threadNumber.getAndIncrement()));
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // Swallow the exception
                log.error(String.format("Thread %s has thrown uncaught exception:%s",
                        t.getName(), e.getMessage()), e);
            }
        });
        return thread;
    }
}