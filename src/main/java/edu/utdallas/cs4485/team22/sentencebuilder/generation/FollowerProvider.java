package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import java.util.List;

/**
 * Read-side contract for sentence generators (Umair WP6).
 * Implemented by the JDBC data layer so generators never touch SQL.
 *
 * @author Umair Malik
 * @date 2026-10-23
 */
public interface FollowerProvider {

    List<Follower> getFollowers(String wordText);

    int getTotalCount(String wordText);

    int getEndCount(String wordText);

    String getRandomStartWord();
}
