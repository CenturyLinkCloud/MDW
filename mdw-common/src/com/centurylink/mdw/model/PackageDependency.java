/*
 * Copyright (C) 2020 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.model;

import com.centurylink.mdw.model.system.BadVersionException;
import com.centurylink.mdw.model.system.MdwVersion;

public class PackageDependency {

    private String pkg;
    public String getPackage() { return pkg; }
    public void setPackage(String packageName) { this.pkg = packageName; }

    private MdwVersion version;
    public MdwVersion getVersion() { return version; }
    public void setVersion(MdwVersion version) { this.version = version; }

    public PackageDependency(String packageName, MdwVersion version) {
        this.pkg = packageName;
        this.version = version;
    }

    public PackageDependency(String dependency) throws BadVersionException {
        String[] segments = dependency.split("\\s+");
        if (segments.length != 2 || !segments[1].startsWith("v"))
            throw new BadVersionException(dependency);
        this.pkg = segments[0];
        this.version = new MdwVersion(segments[1]);
    }

    @Override
    public String toString() {
        return pkg + version.getLabel();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PackageDependency))
            return false;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}