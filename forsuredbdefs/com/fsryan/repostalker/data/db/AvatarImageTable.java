package com.fsryan.repostalker.data.db;

import com.fsryan.forsuredb.annotations.FSColumn;
import com.fsryan.forsuredb.annotations.FSForeignKey;
import com.fsryan.forsuredb.annotations.FSTable;
import com.fsryan.forsuredb.annotations.Unique;
import com.fsryan.forsuredb.api.FSGetApi;
import com.fsryan.forsuredb.api.Retriever;

/**
 * <p>Stores the avatar images associated with the users.
 * <p>Because each user may only have one image, the image githubId is unique
 */
@FSTable("avatar_images")
public interface AvatarImageTable extends FSGetApi {
    @FSForeignKey(
            apiClass = GithubUsersTable.class,
            columnName = "github_id",
            updateAction = "CASCADE",
            deleteAction = "CASCADE"
    )
    @Unique
    @FSColumn("github_id")
    long userId(Retriever retriever);

    @FSColumn("image")
    byte[] image(Retriever retriever);
}
