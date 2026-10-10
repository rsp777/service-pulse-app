import { Pool } from 'pg';

const pool = new Pool({
  host: process.env.DB_HOST || 'ubuntu-dell',
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'ravi',
  database: process.env.DB_NAME || 'postgres',
  ssl: false, // Change to true if using SSL on production
});

export default pool;
