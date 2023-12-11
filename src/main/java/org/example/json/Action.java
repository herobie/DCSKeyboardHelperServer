package org.example.json;

import java.util.List;

public class Action {
    private String name;//左侧support页面显示的状态，如”开“”关“等
    private List<Integer> codes;//操作代码

    public Action(String name, List<Integer> codes) {
        this.name = name;
        this.codes = codes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getCodes() {
        return codes;
    }

    public void setCodes(List<Integer> codes) {
        this.codes = codes;
    }
}
