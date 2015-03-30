/*
 * Copyright 2006-2013 Alessandro Cocco.
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
package jcodecollector.common.bean;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Questa classe incapsula il concetto di "stile". Ogni stile ha un nome ed una
 * serie di keywords, chiavi che saranno colorate all'interno dell'editor.
 *
 * @author Alessandro Cocco me@alessandrococco.com
 */
@Entity
public class Syntax implements Comparable<Syntax>, Serializable {
    private static final long serialVersionUID = 1L;
    /** Il nome del linguaggio a cui appartiene questa sintassi. */
    @Id
    private String name;

    /** Le parole chiave da colorare. */
    @Embedded
    private List<String> keywords;

    protected Syntax() {
        this("", new LinkedList<String>());
    }

    public Syntax(String name) {
        this(name, new LinkedList<String>());
    }

    public Syntax(String name, List<String> keywords) {
        this.name = name;
        this.keywords = new LinkedList<String>(keywords);
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getKeywordsAsString() {
        String k = new String();

        for (String s : keywords) {
            k += s.trim() + ", ";
        }

        return k;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = new LinkedList<String>(keywords);
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Syntax other = (Syntax) obj;
        if (this.name != other.name
                && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(Syntax o) {
        return name.compareToIgnoreCase(o.name);
    }
}
