/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repositories;

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactory;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Vince Bickers
 */
public class ProgrammaticRepositoryIT extends MultiDriverTestClass {

    private static GraphDatabaseService graphDatabaseService;

    private MovieRepository movieRepository;
    private SessionFactory sessionFactory = new SessionFactory("org.springframework.data.neo4j.repositories.domain");
    private PlatformTransactionManager platformTransactionManager = new Neo4jTransactionManager(sessionFactory);
    private Session session;
    private TransactionTemplate transactionTemplate;

    @BeforeClass
    public static void beforeClass(){
        graphDatabaseService = getGraphDatabaseService();
    }

    @Before
    public void init() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
        session = sessionFactory.openSession();
        session.purgeDatabase();
    }

    @Test
    public void canInstantiateRepositoryProgrammatically() {

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

                movieRepository = factory.getRepository(MovieRepository.class);

                Movie movie = new Movie("PF");
                movieRepository.save(movie);
            }
        });

        assertSameGraph(graphDatabaseService, "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }

	/**
     * @see DATAGRAPH-847
     */
    @Test
    @Transactional
    public void shouldBeAbleToDeleteAllViaRepository() {

        RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        User userB = new User("B");
        userRepository.save(userA);
        userRepository.save(userB);

        assertEquals(2, userRepository.count());

        userRepository.deleteAll();
        assertEquals(0, userRepository.count());
    }

    /**
     * @see DATAGRAPH-813
     */
    @Test
    @Transactional
    public void shouldDeleteUserByNameAndReturnCountOfDeletedUsers() {

        RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        userA.setName("A");

        userRepository.save(userA);
        Assert.assertEquals(1, userRepository.count());

        Assert.assertEquals(new Long(1), userRepository.deleteByName("A"));
        Assert.assertEquals(0, userRepository.count());

    }

    /**
     * @see DATAGRAPH-813
     */
    @Test
    @Transactional
    public void shouldDeleteUserByNameAndReturnListOfDeletedUserIds() {

        RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        User userAClone = new User("A");


        userRepository.save(userA);
        userRepository.save(userAClone);

        Assert.assertEquals(2, userRepository.count());

        List<Long> deletedUserIds = userRepository.removeByName("A");
        Assert.assertEquals(2, deletedUserIds.size());

        Assert.assertEquals(userA.getId(), deletedUserIds.get(0));
        Assert.assertEquals(userAClone.getId(), deletedUserIds.get(1));

        Assert.assertEquals(0, userRepository.count());

    }

    @Test
    @Transactional
    public void shouldBeAbleToDeleteUserWithRelationships() {

        RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        User userB = new User("B");

        userA.getFriends().add(userB);
        userB.getFriends().add(userA);

        userRepository.save(userA);

        Assert.assertEquals(2, userRepository.count());

        userRepository.deleteByName("A");

        Assert.assertEquals(1, userRepository.count());

    }

    /**
     * @see DATAGRAPH-813
     */
    @Test
    @Transactional
    public void shouldCountUserByName() {

        RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        userA.setName("A");

        userRepository.save(userA);

        Assert.assertEquals(new Long(1), userRepository.countByName("A"));
    }
}
