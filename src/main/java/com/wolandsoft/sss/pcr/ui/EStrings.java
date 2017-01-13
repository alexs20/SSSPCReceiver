/*
    Copyright 2017 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.wolandsoft.sss.pcr.ui;

import java.util.Locale;
import java.util.ResourceBundle;

public enum EStrings {
    
    msg_system_tray_is_not_supported,
    msg_data_copied,
    lbl_app_name,
    lbl_enable,
    lbl_show_pair_key,
    lbl_exit,
    lbl_error,
    
    ;
    
    private static ResourceBundle mStringsResource = ResourceBundle.getBundle(
	    EStrings.class.getPackage().getName() + ".strings.StringsBundle", Locale.ENGLISH);

    public String toString(){
	return mStringsResource.getString(this.name());
    }
}
