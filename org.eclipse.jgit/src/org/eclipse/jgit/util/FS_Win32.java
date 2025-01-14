/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * FS implementation for Windows
 *
 * @since 3.0
 */
public class FS_Win32 extends FS {

	private volatile Boolean supportSymlinks;

	/**
	 * Constructor
	 */
	public FS_Win32() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param src
	 *            instance whose attributes to copy
	 */
	protected FS_Win32(FS src) {
		super(src);
	}

	public FS newInstance() {
		return new FS_Win32(this);
	}

	public boolean supportsExecute() {
		return false;
	}

	public boolean canExecute(final File f) {
		return false;
	}

	public boolean setExecute(final File f, final boolean canExec) {
		return false;
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}

	@Override
	public boolean retryFailedLockFileCommit() {
		return true;
	}

	@Override
	protected File discoverGitExe() {
		String path = SystemReader.getInstance().getenv("PATH"); //$NON-NLS-1$
		File gitExe = searchPath(path, "git.exe", "git.cmd"); //$NON-NLS-1$ //$NON-NLS-2$

		if (gitExe == null) {
			if (searchPath(path, "bash.exe") != null) { //$NON-NLS-1$
				// This isn't likely to work, but its worth trying:
				// If bash is in $PATH, git should also be in $PATH.
				String w = readPipe(userHome(),
						new String[]{"bash", "--login", "-c", "which git"}, // //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						Charset.defaultCharset().name());
				if (!StringUtils.isEmptyOrNull(w))
					// The path may be in cygwin/msys notation so resolve it right away
					gitExe = resolve(null, w);
			}
		}

		return gitExe;
	}

	@Override
	protected File userHomeImpl() {
		String home = SystemReader.getInstance().getenv("HOME"); //$NON-NLS-1$
		if (home != null)
			return resolve(null, home);
		String homeDrive = SystemReader.getInstance().getenv("HOMEDRIVE"); //$NON-NLS-1$
		if (homeDrive != null) {
			String homePath = SystemReader.getInstance().getenv("HOMEPATH"); //$NON-NLS-1$
			if (homePath != null)
				return new File(homeDrive, homePath);
		}

		String homeShare = SystemReader.getInstance().getenv("HOMESHARE"); //$NON-NLS-1$
		if (homeShare != null)
			return new File(homeShare);

		return super.userHomeImpl();
	}

	@Override
	public ProcessBuilder runInShell(String cmd, String[] args) {
		List<String> argv = new ArrayList<String>(3 + args.length);
		argv.add("cmd.exe"); //$NON-NLS-1$
		argv.add("/c"); //$NON-NLS-1$
		argv.add(cmd);
		argv.addAll(Arrays.asList(args));
		ProcessBuilder proc = new ProcessBuilder();
		proc.command(argv);
		return proc;
	}

	@Override
	public boolean supportsSymlinks() {
		if (supportSymlinks == null)
			detectSymlinkSupport();
		return Boolean.TRUE.equals(supportSymlinks);
	}

	private void detectSymlinkSupport() {
		File tempFile = null;
		try {
			tempFile = File.createTempFile("tempsymlinktarget", ""); //$NON-NLS-1$ //$NON-NLS-2$
			File linkName = new File(tempFile.getParentFile(), "tempsymlink"); //$NON-NLS-1$
			createSymLink(linkName, tempFile.getPath());
			supportSymlinks = Boolean.TRUE;
			linkName.delete();
		} catch (IOException | UnsupportedOperationException e) {
			supportSymlinks = Boolean.FALSE;
		} finally {
			if (tempFile != null)
				try {
					FileUtils.delete(tempFile);
				} catch (IOException e) {
					throw new RuntimeException(e); // panic
				}
		}
	}

	/**
	 * @since 3.3
	 */
	@Override
	public Attributes getAttributes(File path) {
		return FileUtils.getFileAttributesBasic(this, path);
	}
}
