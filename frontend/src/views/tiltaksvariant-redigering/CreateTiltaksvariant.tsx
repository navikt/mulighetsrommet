import React from 'react';
import useTiltaksvariantCreate from '../../hooks/tiltaksvariant/useTiltaksvariantCreate';
import MainView from '../../layouts/MainView';
import TiltaksvariantForm from './TiltaksvariantForm';

export const CreateTiltaksvariant = () => {
  const create = useTiltaksvariantCreate();

  return (
    <MainView title="Opprett tiltaksvariant" dataTestId="header_opprett-tiltaksvariant" tilbakelenke="/">
      <div>
        <TiltaksvariantForm onSubmit={create.mutate} />
      </div>
    </MainView>
  );
};
