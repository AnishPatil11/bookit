import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useLoginMutation } from '../api';
import { setCredentials } from '../authSlice';
import { ErrorNote } from '../components/ui';
import { errorMessage } from '../lib/format';
import { useAppDispatch } from '../store';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [login, { isLoading, error }] = useLoginMutation();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      const res = await login({ email, password }).unwrap();
      dispatch(setCredentials(res));
      navigate(from ?? (res.user.role === 'ADMIN' ? '/admin' : '/'), { replace: true });
    } catch {
      /* the error is shown below */
    }
  }

  return (
    <div className="page max-w-md">
      <h1 className="text-3xl font-bold">Welcome back</h1>
      <p className="mt-1 text-sm text-ink-soft">Sign in to manage your bookings and orders.</p>
      <form onSubmit={submit} className="card mt-6 space-y-4 p-6">
        {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
        <div>
          <label htmlFor="email" className="label">Email</label>
          <input id="email" type="email" required autoComplete="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="password" className="label">Password</label>
          <input id="password" type="password" required autoComplete="current-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn-primary w-full" disabled={isLoading}>{isLoading ? 'Signing in...' : 'Sign in'}</button>
        <button
          type="button"
          className="btn-ghost w-full"
          onClick={() => {
            setEmail('demo@bookit.dev');
            setPassword('Demo@12345');
          }}
        >
          Fill demo account
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-ink-soft">
        New here? <Link to="/register" className="font-medium text-pine underline">Create an account</Link>
      </p>
    </div>
  );
}
