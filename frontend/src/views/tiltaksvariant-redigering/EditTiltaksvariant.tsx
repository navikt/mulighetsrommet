import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import SlettModal from '../../components/modal/SlettModal';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import useTiltaksvariantDelete from '../../hooks/tiltaksvariant/useTiltaksvariantDelete';
import useTiltaksvariantUpdate from '../../hooks/tiltaksvariant/useTiltaksvariantUpdate';
import MainView from '../../layouts/MainView';
import TiltaksvariantForm from './TiltaksvariantForm';

export const EditTiltaksvariant = () => {
  const { id } = useParams<{ id: string }>();

  const [isModalOpen, setIsModalOpen] = useState(false);

  const { data, isLoading, isError } = useTiltaksvariant(id);

  const edit = useTiltaksvariantUpdate(id);
  const deleteMutation = useTiltaksvariantDelete(id);

  return (
    <MainView
      title="Rediger tiltaksvariant"
      dataTestId="header-rediger-tiltaksvariant"
      tilbakelenke={'/tiltaksvarianter/' + id}
    >
      <div>
        <TiltaksvariantForm
          isLoading={isLoading}
          isError={isError}
          tiltaksvariant={data}
          onSubmit={edit.mutate}
          onDelete={() => setIsModalOpen(true)}
        />
      </div>
      <SlettModal
        tittel="Slett tiltaksvariant"
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        handleDelete={deleteMutation.mutate}
      />
    </MainView>
  );
};
