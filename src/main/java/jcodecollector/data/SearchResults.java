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
package jcodecollector.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import jcodecollector.Loader;

import jcodecollector.common.bean.Snippet;

public class SearchResults {

    /** La mappa ordinata che contiene gli snippet suddivisi per categoria. */
    private TreeMap<String, TreeSet<Snippet>> data = null;

    private static SearchResults searchResults = new SearchResults();

    public static SearchResults getInstance() {
        return searchResults;
    }

    private SearchResults() {
        this.data = new TreeMap<String, TreeSet<Snippet>>();
    }

    public ArrayList<Snippet> getSnippets(String category) {
        ArrayList<Snippet> names = new ArrayList<Snippet>();
        TreeSet<Snippet> set = data.get(category);
        if (set != null) {
            for(Snippet snippet : data.get(category)) {
                names.add(snippet);
            }
        }

        return names;
    }

    public ArrayList<String> getCategories() {
        return new ArrayList<String>(data.keySet());
    }

    /**
     * Richiede al database la cancellazione di tutti gli snippet della
     * categoria indicata trovati con l'ultima ricerca.
     *
     * @param category La categoria degli snippet da cancellare.
     */
    public void removeCategory(String category) {
        if (!data.containsKey(category)) {
            return;
        }

        ArrayList<Snippet> array = getSnippets(category);
        Loader.DBMS_INSTANCE.removeSnippets(array);
        data.remove(category);
    }

    public void renameCategory(String oldName, String newName) {
        if (!data.containsKey(oldName)) {
            return;
        }

        // ottengo gli snippet della vecchia categoria
        TreeSet<Snippet> oldValue = data.get(oldName);

        // rimuovo la vecchia categoria dalla mappa
        data.remove(oldName);

        // se la nuova categoria non e' presente la inserisco con tutti gli
        // snippet della vecchia categoria
        if (!data.containsKey(newName)) {
            data.put(newName, oldValue);
        } else {
            // altrimenti le aggiungo i vecchi snippet
            TreeSet<Snippet> newValue = data.get(newName);
            newValue.addAll(oldValue);
            data.put(newName, newValue);
        }

        // fatto questo posso chiedere al dbms di effettuare l'aggiornamento
        Loader.DBMS_INSTANCE.renameCategoryOf(
                data.get(newName), newName);
    }

    public void removeSnippet(Snippet name) {
        Iterator<String> iterator = data.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            TreeSet<Snippet> value = data.get(key);
            if (value.contains(name)) {
                value.remove(name);
                Loader.DBMS_INSTANCE.removeSnippet(name);
            }
        }
    }

    public void updateSnippet(Snippet oldSnippet, Snippet newSnippet) {
        data.get(oldSnippet.getCategory()).remove(oldSnippet);

        if (data.containsKey(newSnippet.getCategory())) {
            data.get(newSnippet.getCategory()).add(newSnippet);
        } else {
            TreeSet<Snippet> value = new TreeSet<Snippet>();
            value.add(newSnippet);
            data.put(newSnippet.getCategory(), value);
        }

        Loader.DBMS_INSTANCE.updateSnippet(oldSnippet, newSnippet);
    }

    public void setData(TreeMap<String, TreeSet<Snippet>> data) {
        this.data = data;
    }

    public int size() {
        return data.size();
    }

    public int countCategories() {
        return data.keySet().size();
    }

    public int countSnippets() {
        int n = 0;

        for (String s : data.keySet()) {
            n += data.get(s).size();
        }

        return n;
    }

    public void clear() {
        data.clear();
    }

    public void setSyntax(String newSyntax, String category, Snippet selected) {
        if (!data.containsKey(category)) {
            return;
        }

        Set<Snippet> snippets = data.get(category);
        snippets.remove(selected);

        Loader.DBMS_INSTANCE.setSyntaxToSnippets(newSyntax, snippets);
    }
}
