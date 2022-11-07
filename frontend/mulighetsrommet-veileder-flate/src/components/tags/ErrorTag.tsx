import { Tag } from '@navikt/ds-react';
import styles from './ErrorTag.module.scss';
import { kebabCase } from '../../utils/Utils';
import { ErrorColored } from '@navikt/ds-icons';

interface Props {
  innhold: string;
  title: string;
}

export const ErrorTag = ({ innhold, title }: Props) => {
  return (
    <Tag
      className={styles.alert_test}
      key={'navenhet'}
      variant="error"
      size="small"
      data-testid={`${kebabCase('filtertag_navenhet')}`}
      title={title}
    >
      <ErrorColored />
      <span style={{ marginLeft: '10px' }}>{innhold}</span>
    </Tag>
  );
};
