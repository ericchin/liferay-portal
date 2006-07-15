/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.mailbox.util;

import java.util.Date;

import com.liferay.util.StringPool;

/**
 * <a href="Envelope.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Alexander Chow
 *
 */
public class MailEnvelope {

	public MailEnvelope() {
		_email = StringPool.BLANK;
		_subject = StringPool.BLANK;
	}

	public String getEmail() {
		return _email;
	}
	
	public void setEmail(String email) {
		_email = email;
	}

	public String getSubject() {
		return _subject;
	}

	public void setSubject(String subject) {
		_subject = subject;
	}

	public Date getDate() {
		return _date;
	}

	public void setDate(Date date) {
		_date = date;
	}
	
	public long getUID() {
		return _UID;
	}
	
	public void setUID(long UID) {
		_UID = UID;
	}

	public boolean isRecent() {
		return _recent;
	}
	
	public void setRecent(boolean recent) {
		_recent = recent;
	}

	public boolean isFlagged() {
		return _flagged;
	}
	
	public void setFlagged(boolean flagged) {
		_flagged = flagged;
	}

	public boolean isAnswered() {
		return _answered;
	}
	
	public void setAnswered(boolean answered) {
		_answered = answered;
	}
	
	private String _email;

	private String _subject;

	private Date _date;
	
	private long _UID;

	private boolean _recent;

	private boolean _flagged;
	
	private boolean _answered;
	
}