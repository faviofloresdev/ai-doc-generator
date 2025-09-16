package com.agent.doc.generator.impl;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.agent.doc.generator.service.GitService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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

            // Hacer fetch del remoto
            git.fetch()
               .setRemote("origin")
               .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
               .call();

            // Verificar si el branch local existe
            boolean branchExists = git.getRepository().findRef(branchName) != null;

            if (branchExists) {
                git.checkout()
                   .setName(branchName)
                   .call();
            } else {
                git.checkout()
                   .setCreateBranch(true)
                   .setName(branchName)
                   .setStartPoint("refs/remotes/origin/" + branchName)
                   .call();
            }

            Iterable<RevCommit> commits = git.log().call();
            for (RevCommit commit : commits) {
                String message = commit.getFullMessage();
                if (message.contains(huId)) {
                    sb.append("Commit: ").append(commit.getName()).append("\n")
                      .append("Mensaje: ").append(message).append("\n\n");

                    // Obtener cambios de archivos
                    try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
                        diffFormatter.setRepository(git.getRepository());
                        diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                        diffFormatter.setDetectRenames(true);

                        RevCommit parent = commit.getParentCount() > 0 ? commit.getParent(0) : null;
                        if (parent != null) {
                            List<DiffEntry> diffs = diffFormatter.scan(parent.getTree(), commit.getTree());
                            for (DiffEntry diff : diffs) {
                                sb.append("Archivo: ").append(diff.getChangeType())
                                  .append(" -> ").append(diff.getNewPath()).append("\n");
                            }
                        }
                    }
                    sb.append("\n---\n\n");
                }
            }

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            return "Error obteniendo cambios de Git: " + e.getMessage();
        }

        return sb.toString();
    }


    private String getDiffs(Git git, String branch) throws Exception {
        Repository repo = git.getRepository();
        git.checkout().setName(branch).call();

        Iterable<RevCommit> commits = git.log().setMaxCount(2).call();
        RevCommit newest = null;
        RevCommit previous = null;
        for (RevCommit c : commits) {
            if (newest == null) newest = c;
            else { previous = c; break; }
        }

        if (newest == null || previous == null) {
            return "⚠️ No hay suficientes commits para comparar.";
        }

        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, previous.getTree());

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, newest.getTree());

            List<DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();

            StringBuilder sb = new StringBuilder("Cambios en la rama " + branch + ":\n");
            try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                df.setRepository(repo);
                for (DiffEntry diff : diffs) {
                    sb.append(diff.getChangeType())
                      .append(": ")
                      .append(diff.getNewPath())
                      .append("\n");
                }
            }
            return sb.toString();
        }
    }
}
