package org.carlspring.strongbox.storage.indexing;

import org.carlspring.strongbox.config.Maven2LayoutProviderTestConfig;
import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.services.ArtifactManagementService;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.testing.TestCaseWithMavenArtifactGenerationAndIndexing;
import org.carlspring.strongbox.util.IndexContextHelper;

import javax.inject.Inject;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.expr.UserInputSearchExpression;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Przemyslaw Fusik
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "test")
@ContextConfiguration(classes = Maven2LayoutProviderTestConfig.class)
@EnabledIf(expression = "#{containsObject('repositoryIndexManager')}", loadContext = true)
@Disabled // Needs more work
/**
 * [ERROR] Tests run: 5, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 1.64 s <<< FAILURE! - in org.carlspring.strongbox.storage.indexing.StrongboxIndexerTest
 * [ERROR] indexerShouldBeCapableToSearchByFQN  Time elapsed: 0.013 s  <<< ERROR!
 * java.lang.NullPointerException
 * 	at org.carlspring.strongbox.storage.indexing.StrongboxIndexerTest.indexerShouldBeCapableToSearchByFQN(StrongboxIndexerTest.java:148)
 *
 * [ERROR] indexerShouldBeCapableToSearchByClassNameFromZippedArtifact  Time elapsed: 0.292 s  <<< ERROR!
 * java.nio.file.NoSuchFileException: /java/opensource/carlspring/strongbox/strongbox-storage/strongbox-storage-layout-providers/strongbox-storage-maven-layout/strongbox-storage-maven-layout-provider/target/strongbox-vault/storages/storage0/injector-releases-5/org/carlspring/properties-injector/1.7
 * 	at org.carlspring.strongbox.storage.indexing.StrongboxIndexerTest.removeRepositories(StrongboxIndexerTest.java:288)
 *
 * [ERROR] indexerShouldBeCapableToSearchByClassName  Time elapsed: 0.003 s  <<< ERROR!
 * java.lang.NullPointerException
 * 	at org.carlspring.strongbox.storage.indexing.StrongboxIndexerTest.indexerShouldBeCapableToSearchByClassName(StrongboxIndexerTest.java:121)
 */
public class StrongboxIndexerTest
        extends TestCaseWithMavenArtifactGenerationAndIndexing
{

    private static final String REPOSITORY_RELEASES_1 = "injector-releases-1";

    private static final String REPOSITORY_RELEASES_2 = "injector-releases-2";

    private static final String REPOSITORY_RELEASES_3 = "injector-releases-3";

    private static final String REPOSITORY_RELEASES_4 = "injector-releases-4";

    private static final String REPOSITORY_RELEASES_5 = "injector-releases-5";

    private static final String REPOSITORY_RELEASES_6 = "injector-releases-6";

    /**
     * org/carlspring/ioc/PropertyValueInjector
     * org/carlspring/ioc/InjectionException
     * org/carlspring/ioc/PropertyValue
     * org/carlspring/ioc/PropertiesResources
     */
    private Resource jarArtifact = new ClassPathResource("artifacts/properties-injector-1.7.jar");

    /**
     * org/carlspring/ioc/PropertyValueInjector
     * org/carlspring/ioc/InjectionException
     * org/carlspring/ioc/PropertyValue
     * org/carlspring/ioc/PropertiesResources
     */
    private Resource zipArtifact = new ClassPathResource("artifacts/properties-injector-1.7.zip");

    @Inject
    private ArtifactManagementService artifactManagementService;

    @Inject
    private Optional<Indexer> indexer;

    @BeforeAll
    public static void cleanUp()
            throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }

    public static Set<MutableRepository> getRepositoriesToClean()
    {
        Set<MutableRepository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY_RELEASES_1, Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY_RELEASES_2, Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY_RELEASES_3, Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY_RELEASES_4, Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY_RELEASES_5, Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY_RELEASES_6, Maven2LayoutProvider.ALIAS));

        return repositories;
    }

    @Test
    @Execution(CONCURRENT)
    public void indexerShouldBeCapableToSearchByClassName()
            throws Exception
    {
        createRepository(STORAGE0, REPOSITORY_RELEASES_1, true);

        Indexer indexer = this.indexer.get();
        RepositoryIndexManager repositoryIndexManager = this.repositoryIndexManager.get();

        artifactManagementService.validateAndStore(STORAGE0,
                                                   REPOSITORY_RELEASES_1,
                                                   "org/carlspring/properties-injector/1.7/properties-injector-1.7.jar",
                                                   jarArtifact.getInputStream());

        String contextId = IndexContextHelper.getContextId(STORAGE0, REPOSITORY_RELEASES_1,
                                                           IndexTypeEnum.LOCAL.getType());
        RepositoryIndexer ri = repositoryIndexManager.getRepositoryIndexer(contextId);
        Query q = indexer.constructQuery(MAVEN.CLASSNAMES, new UserInputSearchExpression("PropertiesResources"));

        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(q, ri.getIndexingContext()));

        assertThat(response.getTotalHitsCount(), CoreMatchers.equalTo(1));
    }

    @Test
    @Execution(CONCURRENT)
    public void indexerShouldBeCapableToSearchByFQN()
            throws Exception
    {
        createRepository(STORAGE0, REPOSITORY_RELEASES_2, true);

        Indexer indexer = this.indexer.get();
        RepositoryIndexManager repositoryIndexManager = this.repositoryIndexManager.get();

        artifactManagementService.validateAndStore(STORAGE0,
                                                   REPOSITORY_RELEASES_2,
                                                   "org/carlspring/properties-injector/1.7/properties-injector-1.7.jar",
                                                   jarArtifact.getInputStream());

        String contextId = IndexContextHelper.getContextId(STORAGE0,
                                                           REPOSITORY_RELEASES_2,
                                                           IndexTypeEnum.LOCAL.getType());
        RepositoryIndexer ri = repositoryIndexManager.getRepositoryIndexer(contextId);
        Query q = indexer.constructQuery(MAVEN.CLASSNAMES,
                                         new UserInputSearchExpression("org.carlspring.ioc.PropertyValueInjector"));

        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(q, ri.getIndexingContext()));

        assertThat(response.getTotalHitsCount(), CoreMatchers.equalTo(1));
    }

    @Test
    @Execution(CONCURRENT)
    public void indexerShouldBeCapableToSearchByFullSha1Hash()
            throws Exception
    {
        createRepositoryWithArtifacts(STORAGE0,
                                      REPOSITORY_RELEASES_3,
                                      true,
                                      "org.carlspring:properties-injector",
                                      "1.8");

        Indexer indexer = this.indexer.get();
        RepositoryIndexManager repositoryIndexManager = this.repositoryIndexManager.get();

        String sha1 = Files.readAllLines(getVaultDirectoryPath().resolve("storages")
                                                                .resolve(STORAGE0)
                                                                .resolve(REPOSITORY_RELEASES_3)
                                                                .resolve("org")
                                                                .resolve("carlspring")
                                                                .resolve("properties-injector")
                                                                .resolve("1.8")
                                                                .resolve("properties-injector-1.8.jar.sha1")
                                                                .toAbsolutePath()).get(0);

        String contextId = IndexContextHelper.getContextId(STORAGE0,
                                                           REPOSITORY_RELEASES_3,
                                                           IndexTypeEnum.LOCAL.getType());

        RepositoryIndexer ri = repositoryIndexManager.getRepositoryIndexer(contextId);
        Query q = indexer.constructQuery(MAVEN.SHA1, new SourcedSearchExpression(sha1));

        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(q, ri.getIndexingContext()));

        assertThat(response.getTotalHitsCount(), CoreMatchers.equalTo(1));
    }

    @Test
    @Execution(CONCURRENT)
    public void indexerShouldBeCapableToSearchByPartialSha1Hash()
            throws Exception
    {
        createRepositoryWithArtifacts(STORAGE0,
                                      REPOSITORY_RELEASES_4,
                                      true,
                                      "org.carlspring:properties-injector",
                                      "1.8");

        Indexer indexer = this.indexer.get();
        RepositoryIndexManager repositoryIndexManager = this.repositoryIndexManager.get();

        String sha1 = Files.readAllLines(getVaultDirectoryPath().resolve("storages")
                                                                .resolve(STORAGE0)
                                                                .resolve(REPOSITORY_RELEASES_4)
                                                                .resolve("org")
                                                                .resolve("carlspring")
                                                                .resolve("properties-injector")
                                                                .resolve("1.8")
                                                                .resolve("properties-injector-1.8.jar.sha1")
                                                                .toAbsolutePath()).get(0);

        String contextId = IndexContextHelper.getContextId(STORAGE0,
                                                           REPOSITORY_RELEASES_4,
                                                           IndexTypeEnum.LOCAL.getType());

        RepositoryIndexer ri = repositoryIndexManager.getRepositoryIndexer(contextId);
        Query q = indexer.constructQuery(MAVEN.SHA1, new UserInputSearchExpression(sha1.substring(0, 8)));

        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(q, ri.getIndexingContext()));

        assertThat(response.getTotalHitsCount(), CoreMatchers.equalTo(1));
    }

    @Test
    @Execution(CONCURRENT)
    public void indexerShouldBeCapableToSearchByClassNameFromZippedArtifact()
            throws Exception
    {
        createRepository(STORAGE0, REPOSITORY_RELEASES_5, true);

        Indexer indexer = this.indexer.get();
        RepositoryIndexManager repositoryIndexManager = this.repositoryIndexManager.get();

        artifactManagementService.validateAndStore(STORAGE0,
                                                   REPOSITORY_RELEASES_5,
                                                   "org/carlspring/properties-injector/1.7/properties-injector-1.7.zip",
                                                   zipArtifact.getInputStream());

        String contextId = IndexContextHelper.getContextId(STORAGE0,
                                                           REPOSITORY_RELEASES_5,
                                                           IndexTypeEnum.LOCAL.getType());
        RepositoryIndexer ri = repositoryIndexManager.getRepositoryIndexer(contextId);
        Query q = indexer.constructQuery(MAVEN.CLASSNAMES, new UserInputSearchExpression("PropertiesResources"));

        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(q, ri.getIndexingContext()));

        assertThat(response.getTotalHitsCount(), CoreMatchers.equalTo(1));
    }

    @Test
    @Execution(CONCURRENT)
    public void indexerShouldBeCapableToSearchByFQNFromZippedArtifact()
            throws Exception
    {
        createRepository(STORAGE0, REPOSITORY_RELEASES_6, true);

        Indexer indexer = this.indexer.get();
        RepositoryIndexManager repositoryIndexManager = this.repositoryIndexManager.get();

        artifactManagementService.validateAndStore(STORAGE0,
                                                   REPOSITORY_RELEASES_6,
                                                   "org/carlspring/properties-injector/1.7/properties-injector-1.7.zip",
                                                   zipArtifact.getInputStream());

        String contextId = IndexContextHelper.getContextId(STORAGE0, REPOSITORY_RELEASES_6,
                                                           IndexTypeEnum.LOCAL.getType());
        RepositoryIndexer ri = repositoryIndexManager.getRepositoryIndexer(contextId);
        Query q = indexer.constructQuery(MAVEN.CLASSNAMES,
                                         new UserInputSearchExpression("org.carlspring.ioc.PropertyValueInjector"));

        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(q, ri.getIndexingContext()));

        assertThat(response.getTotalHitsCount(), CoreMatchers.equalTo(1));
    }

    @AfterEach
    public void removeRepositories()
            throws Exception
    {
        closeIndexersForRepository(STORAGE0, REPOSITORY_RELEASES_1);
        closeIndexersForRepository(STORAGE0, REPOSITORY_RELEASES_2);
        closeIndexersForRepository(STORAGE0, REPOSITORY_RELEASES_3);
        closeIndexersForRepository(STORAGE0, REPOSITORY_RELEASES_4);
        closeIndexersForRepository(STORAGE0, REPOSITORY_RELEASES_5);
        closeIndexersForRepository(STORAGE0, REPOSITORY_RELEASES_6);

        removeRepositories(getRepositoriesToClean());

        cleanUp();
    }

}
