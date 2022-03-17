import { factory, primaryKey } from '@mswjs/data';
import { FactoryAPI, ModelDictionary } from '@mswjs/data/lib/glossary';
import { PrimaryKey } from '@mswjs/data/lib/primaryKey';

export function idAutoIncrement(): PrimaryKey<number> {
  let id = 1;
  return primaryKey(() => id++);
}

export function createMockDatabase<Dictionary extends ModelDictionary>(
  definition: Dictionary,
  addTestData?: (db: FactoryAPI<Dictionary>) => void
) {
  const db = factory(definition);

  if (addTestData) {
    addTestData(db);
  }

  return db;
}
