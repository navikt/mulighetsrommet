import { Tag } from '@navikt/ds-react';
import styles from './ErrorTag.module.scss';
import { kebabCase } from '../../utils/Utils';
import { ErrorColored } from '@navikt/ds-icons';

export const ErrorTag = () => {
  return (
    <Tag
      className={styles.alert_test}
      key={'navenhet'}
      variant="error"
      size="small"
      data-testid={`${kebabCase('filtertag_navenhet')}`}
      title="Brukers oppfølgingsenhet"
    >
      <ErrorColored />
      <span style={{ marginLeft: '10px' }}>Enhet mangler</span>
    </Tag>
  );
};
