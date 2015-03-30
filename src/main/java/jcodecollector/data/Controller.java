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

import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import jcodecollector.Loader;

import jcodecollector.State;
import jcodecollector.common.bean.Snippet;

public class Controller {
    private static Controller controller = new Controller();
    private static SearchResults searchManager = SearchResults.getInstance();
    private static SearchFilter filters = SearchFilter.getInstance();

    private Controller() {
        // do nothing
    }

    public static Controller getInstance() {
        return controller;
    }

    public void removeSnippet(Snippet name) {
        if (State.getInstance().isSearchActive()) {
            searchManager.removeSnippet(name);
        }else {
            Loader.DBMS_INSTANCE.removeSnippet(name);
        }
    }

    public void updateSnippet(Snippet oldSnippet, Snippet newSnippet) {
        if(State.getInstance().isSearchActive()) {
            searchManager.updateSnippet(oldSnippet, newSnippet);
        }else {
            Loader.DBMS_INSTANCE.updateSnippet(oldSnippet, newSnippet);
        }
    }

    public void removeCategory(String text) {
        if(State.getInstance().isSearchActive()) {
            searchManager.removeCategory(text);
        }else {
            Loader.DBMS_INSTANCE.removeCategory(text);
        }
    }

    public void renameCategory(String oldName, String newName) {
        if(State.getInstance().isSearchActive()) {
            searchManager.renameCategory(oldName, newName);
        }else {
            Loader.DBMS_INSTANCE.renameCategory(oldName, newName);
        }
    }

    public void updateSyntax(String newSyntax, String category, Snippet selectedSnippet) {
        if(State.getInstance().isSearchActive()) {
            searchManager.setSyntax(newSyntax, category, selectedSnippet);
        }else {
            Loader.DBMS_INSTANCE.setSyntaxToCategory(newSyntax, category, selectedSnippet);
        }
    }

    /**
     * Restituisce l'elenco delle categorie presenti nel database.
     *
     * @return l'elenco delle categorie presenti nel database
     */
    public List<String> getAllCategories() {
        return Loader.DBMS_INSTANCE.getCategories();
    }

    public List<String> getCategories() {
        return State.getInstance().isSearchActive() ? searchManager.getCategories() : Loader.DBMS_INSTANCE.getCategories();
    }

    public String getCategoryOf(String snippet) {
        return Loader.DBMS_INSTANCE.getCategoryOf(snippet);
    }

    public Snippet getSnippet(String name) {
        return Loader.DBMS_INSTANCE.getSnippet(name);
    }

    public List<Snippet> getSnippetsName(String category) {
        if(State.getInstance().isSearchActive()) {
            return searchManager.getSnippets(category);
        }else {
            return Loader.DBMS_INSTANCE.getSnippetsNames(category);
        }
    }

    public void insertNewSnippet(Snippet newSnippet) {
        Loader.DBMS_INSTANCE.insertNewSnippet(newSnippet);
    }

    public void lockSnippet(Snippet snippet, boolean locked) {
        Loader.DBMS_INSTANCE.lockSnippet(snippet, locked);
    }

    public boolean isSearchActive() {
        return State.getInstance().isSearchActive();
    }

    public void setData(TreeMap<String, TreeSet<Snippet>> data) {
        searchManager.setData(data);
    }

    public int countCategories() {
        return State.getInstance().isSearchActive() ? searchManager.countCategories() : Loader.DBMS_INSTANCE.countCategories();
    }

    public int countSnippets() {
        return State.getInstance().isSearchActive() ? searchManager.countSnippets() : Loader.DBMS_INSTANCE.countSnippets();
    }

    public int size() {
        return countSnippets();
    }

    public int getValue() {
        return State.getInstance().isSearchActive() ? filters.countSearchTypeEnabled() : -1;
    }
}
