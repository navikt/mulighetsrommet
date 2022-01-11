import React from 'react';
import useTiltaksvariantCreate from '../../hooks/tiltaksvariant/useTiltaksvariantCreate';
import MainView from '../../layouts/MainView';
import TiltaksvariantForm from './TiltaksvariantForm';

export const OpprettTiltaksvariant = () => {
  const create = useTiltaksvariantCreate();

  return (
    <MainView title="Opprett tiltaksvariant" dataTestId="header_opprett-tiltaksvariant" tilbakelenke="/">
      <TiltaksvariantForm onSubmit={create.mutate} isEdit={false} />
    </MainView>
  );
};
