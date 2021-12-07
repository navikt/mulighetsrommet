import { DecoratorProps } from './DecoratorProps';

const decoratorConfig = (): DecoratorProps => {
  return {
    appname: 'Arbeidstiltak',
    toggles: {
      visVeileder: true,
    },
  };
};

export default decoratorConfig;
