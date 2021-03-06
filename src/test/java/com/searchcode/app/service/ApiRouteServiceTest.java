package com.searchcode.app.service;

import com.searchcode.app.dao.Repo;
import com.searchcode.app.dto.api.ApiResponse;
import com.searchcode.app.dto.api.RepoResultApiResponse;
import com.searchcode.app.model.RepoResult;
import com.searchcode.app.service.route.ApiRouteService;
import com.searchcode.app.util.UniqueRepoQueue;
import junit.framework.TestCase;
import org.mockito.Matchers;
import org.mockito.Mockito;
import spark.Request;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

public class ApiRouteServiceTest extends TestCase {
    public void testRepositoryReindexApiNotEnabled() {
        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = false;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(null, null);

        assertThat(apiResponse.getMessage()).isEqualTo("API not enabled");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepositoryReindexApiAuthNotEnabledRebuildAllWorks() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);

        when(mockJobService.rebuildAll()).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("reindex forced");
        assertThat(apiResponse.isSucessful()).isEqualTo(true);
        verify(mockJobService, times(1)).rebuildAll();
        verify(mockJobService, times(1)).forceEnqueue();
    }

    public void testRepositoryReindexApiAuthNotEnabledRebuildAllFails() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);

        when(mockJobService.rebuildAll()).thenReturn(false);

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("was unable to force the index");
        assertThat(apiResponse.isSucessful()).isEqualTo(false);

        verify(mockJobService, times(1)).rebuildAll();
        verify(mockJobService, times(0)).forceEnqueue();
    }

    public void testRepositoryReindexApiAuthEnabledPubMissing() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);

        when(mockJobService.rebuildAll()).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("pub is a required parameter");
        assertThat(apiResponse.isSucessful()).isEqualTo(false);
    }

    public void testRepositoryReindexApiAuthEnabledSigMissing() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockJobService.rebuildAll()).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("sig is a required parameter");
        assertThat(apiResponse.isSucessful()).isEqualTo(false);
    }

    public void testRepositoryReindexApiAuthEnabledInvalidSig() {
        JobService mockJobService = Mockito.mock(JobService.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        Request mockRequest = Mockito.mock(Request.class);

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");
        when(mockJobService.rebuildAll()).thenReturn(true);
        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(false);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, mockJobService, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("invalid signed url");
        assertThat(apiResponse.isSucessful()).isEqualTo(false);
    }

    public void testRepositoryReindexApiAuthEnabledValidSig() {
        JobService mockJobService = Mockito.mock(JobService.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        Request mockRequest = Mockito.mock(Request.class);

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");
        when(mockJobService.rebuildAll()).thenReturn(true);
        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, mockJobService, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        ApiResponse apiResponse = apiRouteService.repositoryReindex(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("reindex forced");
        assertThat(apiResponse.isSucessful()).isEqualTo(true);
    }

    /////////////////////////////////////////////////////////////////////

    public void testRepositoryIndexApiNotEnabled() {
        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = false;

        ApiResponse apiResponse = apiRouteService.repositoryIndex(null, null);

        assertThat(apiResponse.getMessage()).isEqualTo("API not enabled");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepositoryIndexApiNoRepositorySupplied() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        when(mockJobService.forceEnqueue(Matchers.<RepoResult>anyObject())).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, mockRepo, null);
        apiRouteService.apiEnabled = true;

        ApiResponse apiResponse = apiRouteService.repositoryIndex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("Was unable to find repository null");
        assertThat(apiResponse.isSucessful()).isEqualTo(false);
    }

    public void testRepositoryIndexApiNoMatchinRepo() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        when(mockJobService.forceEnqueue(Matchers.<RepoResult>anyObject())).thenReturn(true);
        when(mockRequest.queryParams("repoUrl")).thenReturn("test");

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, mockRepo, null);
        apiRouteService.apiEnabled = true;

        ApiResponse apiResponse = apiRouteService.repositoryIndex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("Was unable to find repository test");
        assertThat(apiResponse.isSucessful()).isEqualTo(false);
    }

    public void testRepositoryIndexMatchingRepo() {
        JobService mockJobService = Mockito.mock(JobService.class);
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        when(mockJobService.forceEnqueue(Matchers.<RepoResult>anyObject())).thenReturn(true);
        when(mockRequest.queryParams("repoUrl")).thenReturn("http://test/");
        when(mockRepo.getRepoByUrl("http://test/")).thenReturn(new RepoResult());

        ApiRouteService apiRouteService = new ApiRouteService(null, mockJobService, mockRepo, null);
        apiRouteService.apiEnabled = true;

        ApiResponse apiResponse = apiRouteService.repositoryIndex(mockRequest, null);
        assertThat(apiResponse.getMessage()).isEqualTo("Enqueued repository http://test/");
        assertThat(apiResponse.isSucessful()).isEqualTo(true);
    }

    /////////////////////////////////////////////////////////////////////

    public void testRepoListApiNotEnabled() {
        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = false;

        RepoResultApiResponse apiResponse = apiRouteService.repoList(null, null);

        assertThat(apiResponse.getMessage()).isEqualTo("API not enabled");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoListApiEnabledNoAuth() {
        Request mockRequest = Mockito.mock(Request.class);

        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        RepoResultApiResponse apiResponse = apiRouteService.repoList(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("");
        assertThat(apiResponse.getRepoResultList()).hasSize(0);
        assertThat(apiResponse.isSucessful()).isTrue();
    }

    public void testRepoListApiEnabledAuthMissingPub() {
        Request mockRequest = Mockito.mock(Request.class);

        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        RepoResultApiResponse apiResponse = apiRouteService.repoList(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("pub is a required parameter");
        assertThat(apiResponse.getRepoResultList()).isNull();
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoListApiEnabledAuthMissingSig() {
        Request mockRequest = Mockito.mock(Request.class);

        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("pub")).thenReturn("test");

        RepoResultApiResponse apiResponse = apiRouteService.repoList(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("sig is a required parameter");
        assertThat(apiResponse.getRepoResultList()).isNull();
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoListApiEnabledAuthInvalidSigned() {
        Request mockRequest = Mockito.mock(Request.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(false);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, null, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");

        RepoResultApiResponse apiResponse = apiRouteService.repoList(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("invalid signed url");
        assertThat(apiResponse.getRepoResultList()).isNull();
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoListApiEnabledAuthValid() {
        Request mockRequest = Mockito.mock(Request.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");

        RepoResultApiResponse apiResponse = apiRouteService.repoList(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("");
        assertThat(apiResponse.getRepoResultList()).hasSize(0);
        assertThat(apiResponse.isSucessful()).isTrue();
    }

    /////////////////////////////////////////////////////////////////////

    public void testRepoDeleteApiNotEnabled() {
        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = false;

        ApiResponse apiResponse = apiRouteService.repoDelete(null, null);

        assertThat(apiResponse.getMessage()).isEqualTo("API not enabled");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoDeleteNoAuthNoReponame() {
        Request mockRequest = Mockito.mock(Request.class);

        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        ApiResponse apiResponse = apiRouteService.repoDelete(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("reponame is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoDeleteNoAuthReponame() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        UniqueRepoQueue uniqueRepoQueue = new UniqueRepoQueue(new ConcurrentLinkedQueue<>());

        when(mockRepo.getRepoByName("unit-test")).thenReturn(new RepoResult());

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, uniqueRepoQueue);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("unit-test");

        ApiResponse apiResponse = apiRouteService.repoDelete(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repository queued for deletion");
        assertThat(apiResponse.isSucessful()).isTrue();
        assertThat(uniqueRepoQueue.size()).isEqualTo(1);
    }

    public void testRepoDeleteAuthReponameNoPub() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        UniqueRepoQueue uniqueRepoQueue = new UniqueRepoQueue(new ConcurrentLinkedQueue<>());

        when(mockRepo.getRepoByName("unit-test")).thenReturn(new RepoResult());

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, uniqueRepoQueue);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("reponame")).thenReturn("unit-test");

        ApiResponse apiResponse = apiRouteService.repoDelete(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("pub is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
        assertThat(uniqueRepoQueue.size()).isEqualTo(0);
    }

    public void testRepoDeleteAuthReponameNoSig() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        UniqueRepoQueue uniqueRepoQueue = new UniqueRepoQueue(new ConcurrentLinkedQueue<>());

        when(mockRepo.getRepoByName("unit-test")).thenReturn(new RepoResult());

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, uniqueRepoQueue);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("reponame")).thenReturn("unit-test");

        ApiResponse apiResponse = apiRouteService.repoDelete(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("sig is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
        assertThat(uniqueRepoQueue.size()).isEqualTo(0);
    }

    public void testRepoDeleteAuthReponameFailedAuth() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        UniqueRepoQueue uniqueRepoQueue = new UniqueRepoQueue(new ConcurrentLinkedQueue<>());
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(false);
        when(mockRepo.getRepoByName("unit-test")).thenReturn(new RepoResult());

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, uniqueRepoQueue);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");
        when(mockRequest.queryParams("reponame")).thenReturn("unit-test");

        ApiResponse apiResponse = apiRouteService.repoDelete(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("invalid signed url");
        assertThat(apiResponse.isSucessful()).isFalse();
        assertThat(uniqueRepoQueue.size()).isEqualTo(0);
    }

    public void testRepoDeleteAuthReponameAuth() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        UniqueRepoQueue uniqueRepoQueue = new UniqueRepoQueue(new ConcurrentLinkedQueue<>());
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test&reponame=unit-test", ApiService.HmacType.SHA1)).thenReturn(true);
        when(mockRepo.getRepoByName("unit-test")).thenReturn(new RepoResult());

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, uniqueRepoQueue);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");
        when(mockRequest.queryParams("reponame")).thenReturn("unit-test");

        ApiResponse apiResponse = apiRouteService.repoDelete(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repository queued for deletion");
        assertThat(apiResponse.isSucessful()).isTrue();
        assertThat(uniqueRepoQueue.size()).isEqualTo(1);
    }

    /////////////////////////////////////////////////////////////////////
    // TODO expand on the below tests they do not hit all code paths
    /////////////////////////////////////////////////////////////////////

    public void testRepoAddApiNotEnabled() {
        ApiRouteService apiRouteService = new ApiRouteService();
        apiRouteService.apiEnabled = false;

        ApiResponse apiResponse = apiRouteService.repoAdd(null, null);

        assertThat(apiResponse.getMessage()).isEqualTo("API not enabled");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepoName() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;


        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("reponame is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepoUrl() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repourl is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepotype() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repotype is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepoUsername() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repousername is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepoPassword() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repopassword is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepoSource() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("reposource is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddMissingRepoBranch() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");
        when(mockRequest.queryParams("reposource")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("repobranch is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddNoAuthSucessful() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);

        ApiRouteService apiRouteService = new ApiRouteService(null, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = false;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");
        when(mockRequest.queryParams("reposource")).thenReturn("test");
        when(mockRequest.queryParams("repobranch")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("added repository successfully");
        assertThat(apiResponse.isSucessful()).isTrue();
        verify(mockRepo, times(1)).saveRepo(Matchers.<RepoResult>anyObject());
    }

    public void testRepoAddAuthPubMissing() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(false);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");
        when(mockRequest.queryParams("reposource")).thenReturn("test");
        when(mockRequest.queryParams("repobranch")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("pub is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddAuthSigMissing() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test", ApiService.HmacType.SHA1)).thenReturn(false);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");
        when(mockRequest.queryParams("reposource")).thenReturn("test");
        when(mockRequest.queryParams("repobranch")).thenReturn("test");
        when(mockRequest.queryParams("pub")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("sig is a required parameter");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddAuthInvalidSigned() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test&reponame=test&repourl=test&repotype=test&repousername=test&repopassword=test&reposource=test&repobranch=test", ApiService.HmacType.SHA1)).thenReturn(false);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");
        when(mockRequest.queryParams("reposource")).thenReturn("test");
        when(mockRequest.queryParams("repobranch")).thenReturn("test");
        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("invalid signed url");
        assertThat(apiResponse.isSucessful()).isFalse();
    }

    public void testRepoAddAuthValidSigned() {
        Request mockRequest = Mockito.mock(Request.class);
        Repo mockRepo = Mockito.mock(Repo.class);
        ApiService mockApiService = Mockito.mock(ApiService.class);

        when(mockApiService.validateRequest("test", "test", "pub=test&reponame=test&repourl=test&repotype=test&repousername=test&repopassword=test&reposource=test&repobranch=test", ApiService.HmacType.SHA1)).thenReturn(true);

        ApiRouteService apiRouteService = new ApiRouteService(mockApiService, null, mockRepo, null);
        apiRouteService.apiEnabled = true;
        apiRouteService.apiAuth = true;

        when(mockRequest.queryParams("reponame")).thenReturn("test");
        when(mockRequest.queryParams("repourl")).thenReturn("test");
        when(mockRequest.queryParams("repotype")).thenReturn("test");
        when(mockRequest.queryParams("repousername")).thenReturn("test");
        when(mockRequest.queryParams("repopassword")).thenReturn("test");
        when(mockRequest.queryParams("reposource")).thenReturn("test");
        when(mockRequest.queryParams("repobranch")).thenReturn("test");
        when(mockRequest.queryParams("pub")).thenReturn("test");
        when(mockRequest.queryParams("sig")).thenReturn("test");

        ApiResponse apiResponse = apiRouteService.repoAdd(mockRequest, null);

        assertThat(apiResponse.getMessage()).isEqualTo("added repository successfully");
        assertThat(apiResponse.isSucessful()).isTrue();
        verify(mockRepo, times(1)).saveRepo(Matchers.<RepoResult>anyObject());
    }
}
