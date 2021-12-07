import React from 'react';
import { DecoratorProps } from './DecoratorProps';
import decoratorConfig from './DecoratorConfig';
import Navspa from '@navikt/navspa';

const InternflateDecorator = Navspa.importer<DecoratorProps>('internarbeidsflatefs');

const Decorator = () => {
  const dekoratorConfig = decoratorConfig();
  return <InternflateDecorator {...dekoratorConfig} />;
};

export default Decorator;
