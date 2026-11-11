import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useRegisterMutation } from '../api';
import { setCredentials } from '../authSlice';
import { ErrorNote } from '../components/ui';
import { errorMessage } from '../lib/format';
import { useAppDispatch } from '../store';

export default function Register() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [register, { isLoading, error }] = useRegisterMutation();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      const res = await register({ name, email, password }).unwrap();
      dispatch(setCredentials(res));
      navigate('/', { replace: true });
    } catch {
      /* shown below */
    }
  }

  return (
    <div className="page max-w-md">
      <h1 className="text-3xl font-bold">Create your account</h1>
      <p className="mt-1 text-sm text-ink-soft">Reserve tables and pre-order in a few taps.</p>
      <form onSubmit={submit} className="card mt-6 space-y-4 p-6">
        {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
        <div>
          <label htmlFor="name" className="label">Full name</label>
          <input id="name" required maxLength={100} autoComplete="name" className="input" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div>
          <label htmlFor="email" className="label">Email</label>
          <input id="email" type="email" required autoComplete="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="password" className="label">Password</label>
          <input id="password" type="password" required minLength={8} maxLength={72} autoComplete="new-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
          <p className="mt-1 text-xs text-ink-faint">At least 8 characters.</p>
        </div>
        <button className="btn-primary w-full" disabled={isLoading}>{isLoading ? 'Creating...' : 'Create account'}</button>
      </form>
      <p className="mt-4 text-center text-sm text-ink-soft">
        Already registered? <Link to="/login" className="font-medium text-pine underline">Sign in</Link>
      </p>
    </div>
  );
}
