/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WebAuthnDataRepositoryTest {

    public Context context = ApplicationProvider.getApplicationContext();
    private WebAuthnDataRepository repository;

    @Before
    public void setUpWebAuthnDataRepository() {
        repository = WebAuthnDataRepository.builder().context(context)
                .encryptor(new MockEncryptor()).build();
    }

    @Test
    public void testPersist() {

        PublicKeyCredentialSource source1 = PublicKeyCredentialSource.builder()
                .id("test1".getBytes())
                .rpid("rpid")
                .otherUI("otherUI1")
                .userHandle("userHandle1".getBytes()).build();

        PublicKeyCredentialSource source2 = PublicKeyCredentialSource.builder()
                .id("test2".getBytes())
                .rpid("rpid")
                .otherUI("otherUI2")
                .userHandle("userHandle2".getBytes()).build();

        repository.persist(source1);
        repository.persist(source2);

        List<PublicKeyCredentialSource> sources = repository.getPublicKeyCredentialSource("rpid");
        assertThat(sources).hasSize(2);
        assertThat(sources.get(0).getId()).isEqualTo("test1".getBytes());
        assertThat(sources.get(0).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(0).getOtherUI()).isEqualTo("otherUI1");
        assertThat(sources.get(0).getUserHandle()).isEqualTo("userHandle1".getBytes());
        assertThat(sources.get(0).getType()).isEqualTo("public-key");

        assertThat(sources.get(1).getId()).isEqualTo("test2".getBytes());
        assertThat(sources.get(1).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(1).getOtherUI()).isEqualTo("otherUI2");
        assertThat(sources.get(1).getUserHandle()).isEqualTo("userHandle2".getBytes());
        assertThat(sources.get(1).getType()).isEqualTo("public-key");
    }

    @Test
    public void testPersistWithDifferentRpid() {

        PublicKeyCredentialSource source1 = PublicKeyCredentialSource.builder()
                .id("test1".getBytes())
                .rpid("rpid1")
                .otherUI("otherUI1")
                .userHandle("userHandle1".getBytes()).build();

        PublicKeyCredentialSource source2 = PublicKeyCredentialSource.builder()
                .id("test2".getBytes())
                .rpid("rpid2")
                .otherUI("otherUI2")
                .userHandle("userHandle2".getBytes()).build();

        repository.persist(source1);
        repository.persist(source2);

        List<PublicKeyCredentialSource> sources = repository.getPublicKeyCredentialSource("rpid1");
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getId()).isEqualTo("test1".getBytes());
        assertThat(sources.get(0).getRpid()).isEqualTo("rpid1");
        assertThat(sources.get(0).getOtherUI()).isEqualTo("otherUI1");
        assertThat(sources.get(0).getUserHandle()).isEqualTo("userHandle1".getBytes());
        assertThat(sources.get(0).getType()).isEqualTo("public-key");
    }

    @Test(expected = NullPointerException.class)
    public void testPersistWithNull() {
        repository.persist(null);
    }

    @Test
    public void testReplace() {

        PublicKeyCredentialSource source1 = PublicKeyCredentialSource.builder()
                .id("test1".getBytes())
                .rpid("rpid")
                .otherUI("otherUI1")
                .userHandle("userHandle1".getBytes()).build();

        PublicKeyCredentialSource source2 = PublicKeyCredentialSource.builder()
                .id("test2".getBytes())
                .rpid("rpid")
                .otherUI("otherUI2")
                .userHandle("userHandle2".getBytes()).build();

        repository.persist(source1);
        repository.persist(source2);

        List<PublicKeyCredentialSource> sources = repository.getPublicKeyCredentialSource("rpid");
        assertThat(sources).hasSize(2);
        assertThat(sources.get(0).getId()).isEqualTo("test1".getBytes());
        assertThat(sources.get(0).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(0).getOtherUI()).isEqualTo("otherUI1");
        assertThat(sources.get(0).getUserHandle()).isEqualTo("userHandle1".getBytes());
        assertThat(sources.get(0).getType()).isEqualTo("public-key");

        assertThat(sources.get(1).getId()).isEqualTo("test2".getBytes());
        assertThat(sources.get(1).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(1).getOtherUI()).isEqualTo("otherUI2");
        assertThat(sources.get(1).getUserHandle()).isEqualTo("userHandle2".getBytes());
        assertThat(sources.get(1).getType()).isEqualTo("public-key");

        PublicKeyCredentialSource source3 = PublicKeyCredentialSource.builder()
                .id("test3".getBytes())
                .rpid("rpid")
                .otherUI("otherUI3")
                .userHandle("userHandle1".getBytes()).build();

        repository.persist(source3);

        sources = repository.getPublicKeyCredentialSource("rpid");
        assertThat(sources).hasSize(2);
        assertThat(sources.get(0).getId()).isEqualTo("test2".getBytes());
        assertThat(sources.get(0).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(0).getOtherUI()).isEqualTo("otherUI2");
        assertThat(sources.get(0).getUserHandle()).isEqualTo("userHandle2".getBytes());
        assertThat(sources.get(0).getType()).isEqualTo("public-key");

        assertThat(sources.get(1).getId()).isEqualTo("test3".getBytes());
        assertThat(sources.get(1).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(1).getOtherUI()).isEqualTo("otherUI3");
        assertThat(sources.get(1).getUserHandle()).isEqualTo("userHandle1".getBytes());
        assertThat(sources.get(1).getType()).isEqualTo("public-key");

    }

    @Test
    public void testMaxCredential() {
        repository = WebAuthnDataRepository.builder().context(context)
                .maxCredentials(1)
                .encryptor(new MockEncryptor()).build();

        PublicKeyCredentialSource source1 = PublicKeyCredentialSource.builder()
                .id("test1".getBytes())
                .rpid("rpid")
                .otherUI("otherUI1")
                .userHandle("userHandle1".getBytes()).build();

        PublicKeyCredentialSource source2 = PublicKeyCredentialSource.builder()
                .id("test2".getBytes())
                .rpid("rpid")
                .otherUI("otherUI2")
                .userHandle("userHandle2".getBytes()).build();

        repository.persist(source1);
        repository.persist(source2);

        List<PublicKeyCredentialSource> sources = repository.getPublicKeyCredentialSource("rpid");
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getId()).isEqualTo("test2".getBytes());
        assertThat(sources.get(0).getRpid()).isEqualTo("rpid");
        assertThat(sources.get(0).getOtherUI()).isEqualTo("otherUI2");
        assertThat(sources.get(0).getUserHandle()).isEqualTo("userHandle2".getBytes());
        assertThat(sources.get(0).getType()).isEqualTo("public-key");

    }
}
