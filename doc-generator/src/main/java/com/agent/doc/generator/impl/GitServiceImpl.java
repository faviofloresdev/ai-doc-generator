package com.agent.doc.generator.impl;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.agent.doc.generator.service.GitService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GitServiceImpl implements GitService {
	
    @Value("${git.use.local:false}")
    private boolean useLocal;
    
    @Value("${git.local.path:}")
    private String localRepoPath;
	
    @Value("${git.remote.url:}")
    private String remoteUrl;

    @Value("${git.username:}")
    private String username;

    @Value("${git.token:}")
    private String token;

    @Override
    public String getChangesForBranch(String branchName, String huId) {
        StringBuilder sb = new StringBuilder();

        try (Git git = Git.open(new File(localRepoPath))) {
        	
        	syncBranchWithRemote(git, branchName);

            // Hacer fetch remoto
            git.fetch()
               .setRemote("origin")
               .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
               .call();

            // Checkout del branch
            boolean branchExists = git.getRepository().findRef(branchName) != null;
            if (branchExists) {
                git.checkout().setName(branchName).call();
            } else {
                git.checkout()
                   .setCreateBranch(true)
                   .setName(branchName)
                   .setStartPoint("refs/remotes/origin/" + branchName)
                   .call();
            }

            Repository repo = git.getRepository();

            // Recorrer commits
            Iterable<RevCommit> commits = git.log().call();
            try (RevWalk revWalk = new RevWalk(repo)) {
                for (RevCommit commit : commits) {
                    String message = commit.getFullMessage();

                    if (message.contains(huId)) {
                        sb.append("Commit: ").append(commit.getName()).append("\n")
                          .append("Autor: ").append(commit.getAuthorIdent().getName())
                          .append(" <").append(commit.getAuthorIdent().getEmailAddress()).append(">\n")
                          .append("Fecha: ").append(commit.getAuthorIdent().getWhen()).append("\n")
                          .append("Mensaje: ").append(message).append("\n\n");

                        if (commit.getParentCount() > 0) {
                            RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());

                            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                                 DiffFormatter diffFormatter = new DiffFormatter(out)) {

                                diffFormatter.setRepository(repo);
                                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                                diffFormatter.setDetectRenames(true);

                                List<DiffEntry> diffs = diffFormatter.scan(
                                        parent.getTree(),
                                        commit.getTree()
                                );

                                for (DiffEntry diff : diffs) {
                                    sb.append("Archivo: ")
                                      .append(diff.getChangeType())
                                      .append(" -> ")
                                      .append(diff.getNewPath())
                                      .append("\n");

                                    out.reset();
                                    diffFormatter.format(diff);
                                    sb.append("Diff:\n")
                                      .append(out.toString(StandardCharsets.UTF_8))
                                      .append("\n");
                                }
                            }
                        }

                        sb.append("\n---\n\n");
                    }
                }
            }

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            return "Error obteniendo cambios de Git: " + e.getMessage();
        }

        return sb.toString();
    }

    private void syncBranchWithRemote(Git git, String branchName) throws GitAPIException, IOException {
        // 1. Traer cambios desde remoto
        git.fetch()
           .setRemote("origin")
           .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
           .call();

        // 2. Verificar si el branch existe localmente
        boolean branchExists = git.getRepository().findRef(branchName) != null;

        if (branchExists) {
            git.checkout().setName(branchName).call();
        } else {
            // Crear el branch desde remoto
            git.checkout()
               .setCreateBranch(true)
               .setName(branchName)
               .setStartPoint("refs/remotes/origin/" + branchName)
               .call();
        }

        // 3. Forzar que el branch local se alinee con remoto
        git.reset()
           .setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
           .setRef("origin/" + branchName)
           .call();
    }

}
