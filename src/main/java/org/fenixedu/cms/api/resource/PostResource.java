package org.fenixedu.cms.api.resource;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.cms.api.json.PostAdapter;
import org.fenixedu.cms.api.json.PostFileAdapter;
import org.fenixedu.cms.api.json.PostRevisionAdapter;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.cms.domain.PostFile;
import org.fenixedu.cms.ui.AdminPosts;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

import static org.fenixedu.cms.domain.PermissionEvaluation.ensureCanDoThis;
import static org.fenixedu.cms.domain.PermissionsArray.Permission.DELETE_OTHERS_POSTS;
import static org.fenixedu.cms.domain.PermissionsArray.Permission.DELETE_POSTS;
import static org.fenixedu.cms.domain.PermissionsArray.Permission.DELETE_POSTS_PUBLISHED;
import static org.fenixedu.cms.domain.PermissionsArray.Permission.EDIT_POSTS;

@Path("/cms/posts")
public class PostResource extends BennuRestResource {

    //TODO: check permissions in all methods

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{oid}")
    public JsonElement listLatestVersion(@PathParam("oid") Post post) {
        return view(post, PostAdapter.class);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{oid}")
    public Response deletePost(@PathParam("oid") Post post) {
        ensureCanDoThis(post.getSite(), EDIT_POSTS, DELETE_POSTS);
        if(post.isVisible()) {
            ensureCanDoThis(post.getSite(), EDIT_POSTS, DELETE_POSTS_PUBLISHED);
        }
        if(!Authenticate.getUser().equals(post.getCreatedBy())) {
            ensureCanDoThis(post.getSite(), EDIT_POSTS, DELETE_OTHERS_POSTS);
        }
        post.delete();
        return ok();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{oid}")
    public JsonElement updatePost(@PathParam("oid") Post post, JsonElement json) {
        return updatePostFromJson(post, json);
    }

    private JsonElement updatePostFromJson(Post post, JsonElement json) {
        return view(update(json, post, PostAdapter.class));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{oid}/versions")
    public JsonElement listPostVersions(@PathParam("oid") Post post) {
        return view(post.getRevisionsSet(), PostRevisionAdapter.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{oid}/files")
    public JsonElement listPostFiles(@PathParam("oid") Post post) {
        return view(post.getFilesSet(), PostFileAdapter.class);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{oid}/files")
    public JsonElement addPostFile(@PathParam("oid") Post post, @Context HttpServletRequest request) throws IOException,
    ServletException {
        createFileFromRequest(post, request.getPart("file"));
        return view(post, PostAdapter.class);
    }

    @Atomic(mode = TxMode.WRITE)
    public void createFileFromRequest(Post post, Part part) throws IOException {
        AdminPosts.ensureCanEditPost(post);
        GroupBasedFile groupBasedFile =
                new GroupBasedFile(part.getName(), part.getName(), ByteStreams.toByteArray(part.getInputStream()), Group.logged());

        PostFile postFile = new PostFile(post, groupBasedFile, false, 0);
        post.addFiles(postFile);
    }
}