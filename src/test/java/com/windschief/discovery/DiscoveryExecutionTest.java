package com.windschief.discovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.client.HttpClientService;
import com.windschief.releasedetection.ReleaseDetectionService;
import com.windschief.releasedetection.ReleaseRadarService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.TasteSource;
import com.windschief.task.added_item.AddedItemRepository;

class DiscoveryExecutionTest {
    final TaskRepository tasks = mock(TaskRepository.class);
    final PersonalDiscoveryService discovery = mock(PersonalDiscoveryService.class);
    final ReleaseRadarService radar = new ReleaseRadarService(mock(ReleaseDetectionService.class), tasks,
            mock(SpotifyTokenService.class), mock(SpotifyApi.class), mock(AddedItemRepository.class),
            mock(HttpClientService.class), discovery);

    Task task() {
        Task task = new Task();
        task.setId(42L);
        task.setTasteSource(TasteSource.LIKED_SONGS);
        task.setLastTimeExecuted(Instant.parse("2026-01-01T00:00:00Z"));
        when(tasks.findById(42L)).thenReturn(task);
        return task;
    }

    @Test
    void failuresPreserveTheLastSuccessfulExecutionAndCanBeRetried() throws Exception {
        Task task = task();
        Instant previous = task.getLastTimeExecuted();
        when(discovery.execute(42L)).thenThrow(new DiscoveryException("Reconnect Spotify"));
        radar.execute(task);
        assertEquals(previous, task.getLastTimeExecuted());
        assertTrue(task.isLastRunFailed());
        assertEquals("Reconnect Spotify", task.getLastRunMessage());
        assertFalse(radar.isTaskProcessing(42L));

        doReturn("Added 2 discoveries.").when(discovery).execute(42L);
        radar.execute(task);
        assertTrue(task.getLastTimeExecuted().isAfter(previous));
        assertFalse(task.isLastRunFailed());
        assertFalse(radar.isTaskProcessing(42L));
    }

    @Test
    void submittingReservesTheTaskBeforeReturningAndPreventsConcurrentRuns() throws Exception {
        Task task = task();
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), finished = new CountDownLatch(1);
        when(discovery.execute(42L)).thenAnswer(call -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return "Added 1 discovery.";
        });
        doAnswer(call -> { finished.countDown(); return null; }).when(tasks).persist(any(Task.class));
        try {
            radar.submit(task);
            assertTrue(radar.isTaskProcessing(42L));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            radar.execute(task);
            radar.submit(task);
            verify(discovery, times(1)).execute(42L);
        } finally { release.countDown(); }
        assertTrue(finished.await(5, TimeUnit.SECONDS));
    }
}
