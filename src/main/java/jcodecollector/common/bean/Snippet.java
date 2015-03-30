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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Incapsula il concetto di "snippet". Ogni snippet e' composto dal codice, una
 * categoria, una serie di tag ed un commento addizionale oltre che da un nome
 * (univoco).
 *
 * @author Alessandro Cocco me@alessandrococco.com
 */
/*
internal implementation notes:
- category should be an entity with a name property and operations (e.g. rename) should be managed
much more efficient by updating that property only
*/
@Entity
public class Snippet implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Id dello snippet. */
    @Id
    private Integer id;

    /** La categoria dello snippet. */
    private String category;

    /** Il nome dello snippet. */
    private String name;

    /** I tag dello snippet. */
    @OneToMany
    private List<Tag> tags;

    /** Il codice relativo allo snippet. */
    private String code;

    /** Un commento relativo allo snippet. */
    private String comment;

    /** Lo stile da usare per colorare il codice. */
    private Syntax syntax;

    /** Stato dello snippet. */
    private boolean locked;

    /**
     * Istanzia uno snippet vuoto. I vari attributi dovranno ricevere dei valori
     * validi dai metodi setter.
     */
    protected Snippet() {
        this(-1, "", "", new LinkedList<Tag>(), "", "", new Syntax(""), false);
    }

    public Snippet(int id) {
        this(id, "", "", new LinkedList<Tag>(), "", "", new Syntax(""), false);
    }

    /**
     * Instanzia uno snippet completo dei suoi dati.
     *
     * @param id L'identificatore univoco dello snippet.
     * @param category La categoria dello snippet.
     * @param name Il nome dello snippet,
     * @param tags I tag dello snippet.
     * @param code Il codice dello snippet.
     * @param comment Un commento (opzionale) sullo snippet.
     * @param syntax Lo stile di colorazione sintattica associato.
     * @param locked <code>true</code> se lo snippet e' bloccato,
     *        <code>false</code> altrimenti.
     */
    public Snippet(int id, String category, String name, List<Tag> tags,
            String code, String comment, Syntax syntax, boolean locked) {
        if (syntax == null) {
            syntax = new Syntax("");
        }

        this.category = category;
        this.name = name;
        this.tags = tags;
        this.code = code;
        this.comment = comment;
        this.syntax = syntax;
        this.id = id;
        this.locked = locked;
    }

    public Snippet(int id, String category, String name, List<Tag> tags, String code,
            String comment, Syntax syntax) {
        this(id, category, name, tags, code, comment, syntax, false);
    }

    public Snippet(Snippet snippet) {
        this.id = snippet.getId();
        this.category = snippet.getCategory();
        this.name = snippet.getName();
        this.tags = snippet.getTags();
        this.code = snippet.getCode();
        this.comment = snippet.getComment();
        this.syntax = snippet.getSyntax();
        this.locked = snippet.isLocked();
    }

    /**
     * Restituisce la categoria dello snippet.
     *
     * @return la categoria dello snippet.
     */
    public String getCategory() {
        return this.category;
    }

    /**
     * Assegna allo snippet una nuova categoria.
     *
     * @param category la nuova categoria dello snippet.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Restituisce il nome dello snippet.
     *
     * @return il nome dello snippet.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Assegna un nuovo nome allo snippet.
     *
     * @param name il nuovo nome dello snippet.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Restituisce un clone dell'array dei tag dello snippet.
     *
     * @return un clone dell'array dei tag dello snippet.
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     * Aggiorna i tag dello snippet.
     *
     * @param tags l'array contenente i nuovi tag.
     */
    public void setTags(List<Tag> tags) {
        this.tags = new LinkedList<Tag>(tags);
    }

    /**
     * Restituisce il codice dello snippet.
     *
     * @return il codice dello snippet.
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Aggiorna il codice dello snippet.
     *
     * @param code il nuovo codice dello snippet.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Restituisce il commento assegnato allo snippet. Se non e' presente un
     * commento viene restituita una stringa vuota.
     *
     * @return il commento assegnato allo snippet se presente, una stringa vuota
     *         in caso contrario.
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * Aggiorna il commento dello snippet.
     *
     * @param comment il nuovo commento dello snippet.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Restituisce il nome dello stile di colorazione sintattica assegnato allo
     * snippet.
     *
     * @return il nome dello stile di colorazione sintattica assegnato allo
     *         snippet.
     */
    public Syntax getSyntax() {
        return this.syntax;
    }

    /**
     * Aggiorna il nome dello stile di colorazione sintattica dello snippet.
     *
     * @param syntax Il nuovo nome dello stile di colorazione sintattica dello
     *        snippet.
     */
    public void setSyntax(Syntax syntax) {
        this.syntax = syntax;
    }

    /**
     * Restituisce l'id dello snippet.
     *
     * @return l'id dello snippet.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Imposta l'id dello snippet.
     *
     * @param id il nuovo valore dell'id.
     */
    protected void setId(int id) {
        this.id = id;
    }

    /**
     * Indica se lo snippet e' bloccato (read-only) o meno.
     *
     * @return <code>true</code> se lo snippet e' bloccato, <code>false</code>
     *         altrimenti.
     */
    public boolean isLocked() {
        return this.locked;
    }

    /**
     * Blocca o sblocca lo snippet.
     *
     * @param locked <code>true</code> per bloccare lo snippet,
     *        <code>false</code> per sbloccarlo.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * Restituisce i tag dello snippet sotto forma di un'unica stringa.
     *
     * @return una stringa contenente i tag dello snippet separati da una
     *         virgola ed uno spazio.
     */
    public String getTagsAsString() {
        if(tags.isEmpty()) {
            return "";
        }
        Iterator<Tag> tagsItr = tags.iterator();
        StringBuilder temp = new StringBuilder();
        temp.append(tagsItr.next().getName());
        while(tagsItr.hasNext()) {
            temp.append(", ");
            temp.append(tagsItr.next().getName());
        }
        return temp.toString();
    }

    public void addTag(Tag newTag) {
        this.tags.add(newTag);
    }

    @Override
    public String toString() {
        return category + "," + name;
    }
}
