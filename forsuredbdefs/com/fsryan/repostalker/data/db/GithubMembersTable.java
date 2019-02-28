package com.fsryan.repostalker.data.db;

import com.fsryan.repostalker.data.GithubMember;
import com.fsryan.forsuredb.annotations.*;
import com.fsryan.forsuredb.api.FSDocStoreGetApi;
import com.fsryan.forsuredb.api.Retriever;

@FSTable("github_members")
@FSPrimaryKey("github_id")
public interface GithubMembersTable extends FSDocStoreGetApi<GithubMember> {
    Class BASE_CLASS = GithubMember.class;

    /**
     * <p>The {@link FSColumn#documentValueAccess()} property is a pretty slick
     * way to flatten an arbitrarily deeply nested structure in such a way that
     * you can index and search on some nested field in the document without
     * deserializing each document.
     * <p>It's useful when . . .
     * <ul>
     *   <li>
     *     you will want a specific part of the document in some cases, but not
     *     in others.
     *   </li>
     *   <li>
     *     you want an index on a nested field in the document.
     *   </li>
     *   <li>
     *     you want to filter on a nested field in the document.
     *   </li>
     * </ul>
     * @param retriever The {@link Retriever} that has a record of the
     *                  github_members table
     * @return the avatar url associated with the github member
     */
    @FSColumn(value = "avatar_url", documentValueAccess = {"getAvatarUrl"})
    @Unique
    @Index
    String avatarUrl(Retriever retriever);

    @FSColumn(value = "type", documentValueAccess = {"getType"})
    @Index
    String type(Retriever retriever);

    @FSColumn(value = "username", documentValueAccess = {"getLogin"})
    @Unique
    @Index
    String userName(Retriever retriever);

    @FSColumn(value = "github_id", documentValueAccess = {"getId"})
    long githubId(Retriever retriever);

    /**
     * <p>This is kind of a trick to allow for sorting consistently while
     * ignoring case. This duplicates the login information, but forces
     * the case to lower case so that ordering can occur in a case-
     * insensitive way when ordered on this column and in a case-sensitive
     * way when on {@link #userName(Retriever)}
     * <p>The trick is that forsuredb allows you to store nested fields of the
     * document that are at top level for indexing/searching purposes. The only
     * caveat is that all access must be done by some sequence of no-arg method
     * calls. These no-arg methods need not necessarily access fields. They can
     * instead modify the data that is to be stored at top level should that be
     * desirable. This is one such use case wherein modifying the value to be
     * stored facilitates some useful behavior.
     * @param retriever The {@link Retriever} that has a record of the
     *                  github_members table
     * @return the lowercase user name
     * @see #avatarUrl(Retriever) for an explanation of
     * {@link FSColumn#documentValueAccess()}
     */
    @FSColumn(value = "lowercase_username", documentValueAccess = {"getLogin", "toLowerCase"})
    @Index
    String lowerCaseUserName(Retriever retriever);
}
