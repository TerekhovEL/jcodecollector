package jcodecollector.data;

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

import jcodecollector.exceptions.DirectoryCreationException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;


import jcodecollector.common.bean.Snippet;
import jcodecollector.common.bean.Snippet_;
import jcodecollector.common.bean.Tag;
import jcodecollector.common.bean.Tag_;
import jcodecollector.data.settings.ApplicationSettings;
import jcodecollector.io.PackageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBMS {
    private static final String DBMS_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final Logger logger = LoggerFactory.getLogger(DBMS.class);
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    private void init() throws ClassNotFoundException {
        String connectionURL = "jdbc:derby:";
        String databasePath = ApplicationSettings.getInstance().getDatabasePath() + "jCodeCollector";
        connectionURL += databasePath;
        connectionURL += File.separator + ApplicationSettings.DB_DIR_NAME;

        Class.forName(DBMS_DRIVER);

        /* Se le cartelle che portano alla cartella del database non esistono le
         * creo: prima di creare il database㼠? importante le cartelle siano
         * pronte in quanto derby non le crea in automatico */
        File databaseDirectory = new File(databasePath);
        if (!databaseDirectory.exists()) {
            if (!databaseDirectory.mkdirs()) {
                /* se non riesco a creare le cartelle do un messaggio all'utente
                 * ed esco: non dovrebbe mai fallire, a meno che non ci sia un
                 * problema di permessi */
                throw new DirectoryCreationException(databaseDirectory);
            }
        }

        connectionURL += ";create=true";
        logger.debug("CONNECTION URL: " + connectionURL);


        Map<String, Object> configOverrides = new HashMap<String, Object>();
        configOverrides.put("javax.persistence.jdbc.url", connectionURL);
        configOverrides.put("javax.persistence.jdbc.user", "app");
        configOverrides.put("javax.persistence.jdbc.driver", DBMS_DRIVER);
        configOverrides.put("javax.persistence.jdbc.password", "");
        entityManagerFactory =
                Persistence.createEntityManagerFactory("jcodecollector", configOverrides);
        entityManager = entityManagerFactory.createEntityManager(configOverrides);

        /* Creo le tabelle SNIPPETS e TAGS e inserisco gli snippet di esempio.
         * Se createTables() restituisce false le tabelle sono state create
         * durante una precedente esecuzione. */
        insertDefaultSnippets();
    }

    /** Inserisce nel database gli snippet di esempio. */
    private void insertDefaultSnippets() {
        try {
            List<Snippet> snippets = PackageManager.readPackage(new File(("../default_snippets.jccp")));
            if(snippets != null) {
                for (Snippet s : snippets) {
                    insertNewSnippet(s);
                }
            }
        } catch (Exception ex) {
            logger.warn("cannot find default snippets file", ex);
        }
    }

    public void resetConnection() throws ClassNotFoundException {
        entityManager.flush();
        entityManager.close();

        String databasePath = ApplicationSettings.getInstance().getDatabasePath() + "jCodeCollector";
        String connectionURL = "jdbc:derby:";
        connectionURL += databasePath;
        connectionURL += File.separator + ApplicationSettings.DB_DIR_NAME;
        logger.debug("CONNECTION URL: " + connectionURL);

        File databaseDirectory = new File(databasePath);
        if (!databaseDirectory.exists()) {
            if (!databaseDirectory.mkdirs()) {
                logger.error(String.format("error creating dirs %s", databaseDirectory));
                throw new DirectoryCreationException(databaseDirectory);
            }
        }

        init();
    }

    /**
     * Returns the ID of the snippet.
     *
     * @param name The snippet.
     * @return the ID if the snippet is available, -1 otherwise
     */
    public int getSnippetId(String name) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(snippet).where(criteriaBuilder.equal(snippet.get("name"), name));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();

        if(resultList.isEmpty()) {
            throw new IllegalArgumentException(String.format("snippet with name '%s' not found", name));
        }
        if(resultList.size() > 1) {
            throw new IllegalStateException(String.format("more than one snippet with name '%s' found", name));
        }
        int retValue = resultList.get(0).getId();
        return retValue;
    }

    /**
     * Restituisce la lista di tutte le categorie presenti nel database.
     *
     * @return la lista di tutte le categorie presenti nel database
     */
    public List<String> getCategories() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(snippet.get(Snippet_.category)).distinct(true).orderBy(criteriaBuilder.asc(snippet.get(Snippet_.category)));
        TypedQuery<String> q = entityManager.createQuery(query);
        List<String> resultList = q.getResultList();

        // dopo l'ordinamento metto "Uncategorized" alla fine
        if (resultList.contains("Uncategorized")) {
            resultList.remove("Uncategorized");
            resultList.add("Uncategorized");
        }

        return resultList;
    }

    /**
     * Restituisce la categoria a cui appartiene lo snippet indicato.
     *
     * @param snippetName Il nome dello snippet di cui cercare la categoria.
     * @return la categoria a cui appartiene lo snippet indicato
     */
    public String getCategoryOf(String snippetName) {
        Snippet snippet = entityManager.find(Snippet.class, snippetName);
        return snippet.getCategory();
    }

    /**
     * Inserts a new snippet into the database.
     *
     * @param newSnippet Lo snippet da inserire (o aggiornare)
     */
    public void insertNewSnippet(Snippet newSnippet) {
        entityManager.persist(newSnippet);
    }

    public void updateSnippet(Snippet oldSnippet, Snippet newSnippet) {
        if(!oldSnippet.equals(newSnippet)) {
            //test whether tags collection are disjoint most likely has the same
            //runtime as two for loops
            for(Tag oldTag : oldSnippet.getTags()) {
                if(!newSnippet.getTags().contains(oldTag)) {
                    oldSnippet.getTags().remove(oldTag);
                }
            }
            for(Tag newTag : newSnippet.getTags()) {
                if(!oldSnippet.getTags().contains(newTag)) {
                    oldSnippet.getTags().add(newTag);
                }
            }
        }
        entityManager.merge(oldSnippet);
    }

    /**
     * Returns all snippet of category.
     *
     * @param category The category.
     * @return an <code>ArrayList</code> of all snippet of category
     */
    public List<Snippet> getSnippetsNames(String category) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(snippet).where(criteriaBuilder.equal(snippet.get(Snippet_.category), category));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();
        return resultList;
    }

    /**
     * Restituisce tutti gli snippet presenti nel database.
     *
     * @return tutti gli snippet presenti nel database
     */
    public List<Snippet> getAllSnippets() {
        List<String> categories = getCategories();
        List<Snippet> snippets = new ArrayList<Snippet>();

        for (String category : categories) {
            snippets.addAll(getSnippets(category));
        }

        return snippets;
    }

    public Snippet getSnippet(String name) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippetQuery = query.from(Snippet.class);
        query.select(snippetQuery).where(criteriaBuilder.equal(snippetQuery.get(Snippet_.name), name));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();
        if(resultList.isEmpty()) {
            return null;
        }
        Snippet snippet = resultList.get(0);
        return snippet;
    }

    @Override
    protected void finalize() throws Throwable {
        entityManager.flush();
        entityManager.close();
        entityManagerFactory.close();
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Restituisce tutti gli {@link Snippet} che appartengono alla categoria
     * indicata.
     *
     * @param category La categoria di cui restituire gli snippet.
     * @return un {@link ArrayList} contentente tutti gli {@link Snippet} che
     *         appartengono alla categoria indicata
     */
    public List<Snippet> getSnippets(String category) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(query.from(Snippet.class)).where(criteriaBuilder.equal(snippet.get(Snippet_.category), category));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();
        return resultList;
    }

    /**
     * Elimina dal database lo snippet indicato.
     *
     * @param name Il nome (primary key) dello snippet da eliminare.
     */
    public void removeSnippet(Snippet snippet) {
        entityManager.remove(snippet);
    }

    /**
     * Elimina dal database gli snippet indicati.
     *
     * @param snippets I nomi degli snippet da rimuovere.
     */
    public void removeSnippets(List<Snippet> snippets) {
        for (Snippet snippet : snippets) {
            removeSnippet(snippet);
        }
    }

    public void renameCategory(String oldName, String newName) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(query.from(Snippet.class)).where(criteriaBuilder.equal(snippet.get(Snippet_.category), oldName));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();

        for(Snippet result : resultList) {
            result.setCategory(newName);
        }
    }

    public void renameCategoryOf(Set<Snippet> snippets, String category) {
        for(Snippet snippet : snippets) {
            snippet.setCategory(category);
        }
    }

    public void removeCategory(String name) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(query.from(Snippet.class)).where(criteriaBuilder.equal(snippet.get(Snippet_.category), name));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();

        for(Snippet result : resultList) {
            result.setCategory(null);
        }
    }

    // /**
    // * Inserisce nel database un nuovo stile di colorazione sintattica.
    // *
    // * @param syntax Lo stile di colorazione sintattica da inserire.
    // * @return <code>true</code> se l'operazione e' andata a buon fine,
    // * <code>false</code> altrimenti
    // */
    // public boolean insertNewSyntax(Syntax syntax) {
    // try {
    // String[] keywords = syntax.getKeywords();
    //
    // // inserisco ID e nome stile nella tabella degli stili
    // PreparedStatement preparedStatement =
    // connection.prepareStatement("insert into styles values (DEFAULT, ?)");
    // preparedStatement.setString(1, syntax.getName());
    // preparedStatement.execute();
    //
    // // ottengo l'ID dell'ultimo snippet inserito
    // preparedStatement =
    // connection.prepareStatement("select id_style from styles where style_name = ?");
    // preparedStatement.setString(1, syntax.getName());
    //
    // ResultSet resultSet = preparedStatement.executeQuery();
    // resultSet.next();
    //
    // final int ID_STYLE = resultSet.getInt("id_style");
    //
    // StringBuilder temp = new StringBuilder();
    // for (int i = 0; i < keywords.length; i++) {
    // temp.append("(" + ID_STYLE + ", ?) " + ((i < keywords.length - 1) ?
    // ", " : " "));
    // }
    //
    // // inserisco le parole chiave nella tabelle delle keyword
    // preparedStatement =
    // connection.prepareStatement("insert into keywords values " +
    // temp.toString());
    // for (int i = 0; i < keywords.length; i++) {
    // preparedStatement.setString(i + 1, keywords[i]);
    // }
    //
    // preparedStatement.execute();
    // } catch (SQLException ex) {
    // System.err.println("insertStyle(): " + ex);
    // return false;
    // }
    //
    // return true;
    // throw new UnsupportedOperationException("DO NOT USE THIS METHOD");
    // }

    public int countSnippets() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(query.from(Snippet.class));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();
        return resultList.size();
    }

    public int countCategories() {
        List<String> categories = getCategories();
        return categories.size();
    }

    /**
     * Restituisce tutti i tag degli <code>Snippet</code> della categoria
     * indicata.
     *
     * @param category la categoria di cui trovare tutti k tag
     * @return un <code>ArrayList</code> contenente tutti k <code>Tag</code>
     *         trovati
     */
    public List<Tag> getTags(String category) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
        Root<Snippet> snippet = query.from(Snippet.class);
        query.select(query.from(Snippet.class)).where(criteriaBuilder.equal(snippet.get(Snippet_.category), category));
        TypedQuery<Snippet> q = entityManager.createQuery(query);
        List<Snippet> resultList = q.getResultList();

        List<Tag> retValue = new LinkedList<Tag>();
        for(Snippet result : resultList) {
            retValue.addAll(result.getTags());
        }
        return retValue;
    }

    public List<Tag> getAllTags() {
        List<String> categories = getCategories();

        List<Tag> tags = new ArrayList<Tag>();
        for (String category : categories) {
            tags.addAll(getTags(category));
        }

        return tags;
    }

    /**
     * Blocca/sblocca lo snippet indicato.
     *
     * @param snippet Lo snippet da bloccare/sbloccare.
     * @param locked <code>true</code> per bloccare lo snippet,
     *        <code>false</code> per sbloccarlo.
     * @return <code>true</code> se l'operazione viene eseguita correttamente,
     *         <code>false</code> altrimenti
     */
    public void lockSnippet(Snippet snippet, boolean locked) {
        snippet.setLocked(true);
    }

    public void setSyntaxToCategory(String syntax, String category, Snippet selectedSnippet) {
        Set<Snippet> snippets = new HashSet<Snippet>(getSnippetsNames(category));
        snippets.remove(selectedSnippet);
        setSyntaxToSnippets(syntax, snippets);
    }

    public void setSyntaxToSnippets(String syntax, Set<Snippet> snippets) {
        for(Snippet snippet : snippets) {
            snippet.setSyntax(syntax);
        }
    }

    public TreeMap<String, TreeSet<Snippet>> search(String[] keywords, int search) {
        if (!ApplicationSettings.getInstance().isSearchCaseSensitive()) {
            for (int i = 0; i < keywords.length; i++) {
                keywords[i] = keywords[i].toUpperCase();
            }
        }

        List<Snippet> results = new LinkedList<Snippet>();
        if (ApplicationSettings.getInstance().isSearchInCodeEnabled()) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
            Root<Snippet> snippet = query.from(Snippet.class);
            query.select(query.from(Snippet.class));
            List<Predicate> predicates = new LinkedList<Predicate>();
            for(String keyword : keywords) {
                Predicate predicate = criteriaBuilder.like(snippet.get(Snippet_.code), keyword);
                predicates.add(predicate);
            }
            query.where(criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()])));
            TypedQuery<Snippet> q = entityManager.createQuery(query);
            List<Snippet> resultList = q.getResultList();
            results.addAll(resultList);
            search--;
        }

        // cerco all'interno dei nomi degli snippet
        if (ApplicationSettings.getInstance().isSearchInNameEnabled()) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
            Root<Snippet> snippet = query.from(Snippet.class);
            query.select(query.from(Snippet.class));
            List<Predicate> predicates = new LinkedList<Predicate>();
            for(String keyword : keywords) {
                Predicate predicate = criteriaBuilder.like(snippet.get(Snippet_.name), keyword);
                predicates.add(predicate);
            }
            query.where(criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()])));
            TypedQuery<Snippet> q = entityManager.createQuery(query);
            List<Snippet> resultList = q.getResultList();
            results.addAll(resultList);
            search--;
        }

        // cerco tra i commenti
        if (ApplicationSettings.getInstance().isSearchInCommentEnabled()) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
            Root<Snippet> snippet = query.from(Snippet.class);
            query.select(query.from(Snippet.class));
            List<Predicate> predicates = new LinkedList<Predicate>();
            for(String keyword : keywords) {
                Predicate predicate = criteriaBuilder.like(snippet.get(Snippet_.comment), keyword);
                predicates.add(predicate);
            }
            query.where(criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()])));
            TypedQuery<Snippet> q = entityManager.createQuery(query);
            List<Snippet> resultList = q.getResultList();
            results.addAll(resultList);
            search--;
        }

        // cerco tra i tag
        if (ApplicationSettings.getInstance().isSearchInTagsEnabled()) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Snippet> query = criteriaBuilder.createQuery(Snippet.class);
            Root<Snippet> snippetQuery = query.from(Snippet.class);
            Root<Tag> tagQuery = query.from(Tag.class);
            query.select(query.from(Snippet.class));
            List<Predicate> predicates = new LinkedList<Predicate>();
            for(String keyword : keywords) {
                Predicate predicate = criteriaBuilder.like(tagQuery.get(Tag_.name), keyword);
                predicates.add(predicate);
            }
            query.where(criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()])), criteriaBuilder.and(criteriaBuilder.in(snippetQuery.get(Snippet_.tags))));
            TypedQuery<Snippet> q = entityManager.createQuery(query);
            List<Snippet> resultList = q.getResultList();
            results.addAll(resultList);
            search--;
        }

        TreeMap<String, TreeSet<Snippet>> data;

        if (!ApplicationSettings.getInstance().isSearchCaseSensitive()) {
            for (int i = 0; i < keywords.length; i++) {
                keywords[i] = keywords[i].toUpperCase();
            }
        }

        // costruisco una mappa (categoria, elenco snippet) coi risultati
        // della ricerca
        data = new TreeMap<String, TreeSet<Snippet>>();

        for(Snippet snippet : results) {
            String category = snippet.getCategory();
            TreeSet<Snippet> snippets = data.get(category);
            if(snippets == null) {
                snippets = new TreeSet<Snippet>();
                data.put(category, snippets);
            }
            snippets.add(snippet);
        }
        return data;
    }

    /** The instance of the dbms manager. */
    private static DBMS dbms = null;

    /**
     * Initializes the dbms manager.
     */
    private DBMS() throws ClassNotFoundException {
        init();
    }

    /**
     * Returns the istance of the dbms manager.
     *
     * @return the istance of the dbms manager
     */
    public static DBMS getInstance() throws ClassNotFoundException {
        if(dbms == null) {
            dbms = new DBMS();
        }
        return dbms;
    }

}
