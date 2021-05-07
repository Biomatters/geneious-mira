package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.plugin.Assembler;
import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;

/**
 * Copyright (C) 2014, Biomatters Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * In order to get Mira 4.0 to compile under cygwin, we made the following changes:
 * In src/stdinc/defines.H add the following 3 lines before #include <csignal> :
 * #include <cstdlib>
 * #include <cstdio>
 * #include <errno.h>
 *
 * In configure change -std=c++0x to -std=gnu++0x
 *
 */
public class MiraPlugin extends GeneiousPlugin {

    public static final String PLUGIN_VERSION = "1.1.2";

    @Override
    public String getName() {
        return "MIRA de novo assembler";
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public String getHelp() {
        return getName();
    }

    @Override
    public String getAuthors() {
        return "Biomatters Ltd.";
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getMinimumApiVersion() {
        return "4.712";
    }

    @Override
    public int getMaximumApiVersion() {
        return 4;
    }

    @Override
    public Assembler[] getAssemblers() {
        return new Assembler[]{
                new MiraAssembler()
        };
    }
}
