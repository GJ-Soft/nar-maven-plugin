/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks;

import java.util.List;

/**
 * @author Curt Arnold
 */
public final class DependencyInfo {
  private final/* final */String includePathIdentifier;
  private final/* final */String[] includes;
  private final/* final */String source;
  private final/* final */long sourceLastModified;
  private final/* final */String[] sysIncludes;
  // FREEHEP
  private Object tag = null;

  public DependencyInfo(final String includePathIdentifier, final String source, final long sourceLastModified,
      final List<String> includes, final List<String> sysIncludes) {
    if (source == null) {
      throw new NullPointerException("source");
    }
    if (includePathIdentifier == null) {
      throw new NullPointerException("includePathIdentifier");
    }
    this.source = source;
    this.sourceLastModified = sourceLastModified;
    this.includePathIdentifier = includePathIdentifier;
    this.includes = new String[includes.size()];
    this.sysIncludes = new String[sysIncludes.size()];
    // FREEHEP
    includes.toArray(this.includes);
    sysIncludes.toArray(this.sysIncludes);
  }

  // ENDFREEHEP
  public String getIncludePathIdentifier() {
    return this.includePathIdentifier;
  }

  public String[] getIncludes() {
    return this.includes.clone();
  }

  public String getSource() {
    return this.source;
  }

  public long getSourceLastModified() {
    return this.sourceLastModified;
  }

  public String[] getSysIncludes() {
    return this.sysIncludes.clone();
  }

  // BEGINFREEHEP
  /**
   * Returns true, if dependency info is tagged with object t.
   * 
   * @param t
   *          object to compare with
   * 
   * @return boolean, true, if tagged with t, otherwise false
   */
  public boolean hasTag(final Object t) {
    return this.tag == t;
  }

  public void setTag(final Object t) {
    this.tag = t;
  }
}
