package com.fsryan.repostalker.data.db;

import com.fsryan.repostalker.data.GithubUser;
import com.fsryan.forsuredb.annotations.*;
import com.fsryan.forsuredb.api.FSDocStoreGetApi;
import com.fsryan.forsuredb.api.Retriever;

@FSTable("github_users")
@FSPrimaryKey("github_id")
public interface GithubUsersTable extends FSDocStoreGetApi<GithubUser> {
    Class BASE_CLASS = GithubUser.class;

    @FSColumn(value = "avatar_url", documentValueAccess = {"getAvatarUrl"})
    @Unique
    @Index
    String avatarUrl(Retriever retriever);

    @FSColumn(value = "email", documentValueAccess = {"getEmail"})
    @Unique
    @Index
    String email(Retriever retriever);

    @FSColumn(value = "location", documentValueAccess = {"getLocation"})
    @Index
    String location(Retriever retriever);

    @FSColumn(value = "type", documentValueAccess = {"getType"})
    @Index
    String type(Retriever retriever);

    @FSColumn(value = "username", documentValueAccess = {"getLogin"})
    @Unique
    @Index
    String userName(Retriever retriever);

    @FSColumn(value = "github_id", documentValueAccess = {"getId"})
    long githubId(Retriever retriever);
}
