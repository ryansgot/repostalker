package com.fsryan.repostalker.data.db;

import com.fsryan.forsuredb.annotations.*;
import com.fsryan.forsuredb.api.FSGetApi;
import com.fsryan.forsuredb.api.Retriever;

import java.util.Date;

/**
 * <p>A join table between github_users and github_members that allows allows
 * for an NxN relationship between the two. Users are defined in the
 * github_users table. Followers are defined in the github_members table.
 * <p>The members of an organization and the followers of an organization are
 * lumped together here for expedience.
 * <p>The two columns that define the follower relationship are a composite
 * primary key
 */
@FSTable("followers")
@FSPrimaryKey(value = {"user_id", "follower_id"})
public interface FollowersTable extends FSGetApi {
    @FSColumn("user_id")
    @FSForeignKey(
            apiClass = GithubUsersTable.class,
            columnName = "github_id",
            updateAction = "CASCADE",
            deleteAction = "CASCADE"
    )
    long userId(Retriever retriever);

    @FSColumn("follower_id")
    @FSForeignKey(
            apiClass = GithubMembersTable.class,
            columnName = "github_id",
            updateAction = "CASCADE",
            deleteAction = "CASCADE"
    )
    long followerId(Retriever retriever);

    /**
     * <p>Since there will be many followers per user, it makes sense to mark
     * the date that each was synchronized so that old entries can be removed
     * from the database.
     * @param retriever the {@link Retriever} containing a record of the
     *                  followers table
     * @return the date the follower was synchronized
     */
    @FSColumn("synchronized_date")
    @Index
    Date synchronizedDate(Retriever retriever);
}
