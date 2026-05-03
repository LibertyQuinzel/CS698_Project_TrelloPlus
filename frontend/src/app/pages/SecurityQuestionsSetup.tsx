import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { ShieldCheck, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { toast } from 'sonner';
import { apiService, type SecurityQuestionsResponse } from '../services/api';

export const SYSTEM_SECURITY_QUESTIONS = [
  'Name a teacher or mentor who inspired you',
  'What was your first job?',
  'What was the name of your first pet?',
  'What city were you born in?',
  'What is your favorite book?',
  'What was the model of your first car?',
  'What was your childhood nickname?',
  'What is the name of the street you grew up on?',
  'What was the name of your elementary school?',
  'What is your favorite movie?',
  "What is your mother's maiden name?",
  'What was the first concert you attended?',
];

const DEFAULT_SECURITY_QUESTION_1 = 'Name a teacher or mentor who inspired you';
const DEFAULT_SECURITY_QUESTION_2 = 'What was the name of your first pet?';

interface SecurityQuestionsSetupProps {
  mode?: 'standalone' | 'embedded';
  initialQuestions?: SecurityQuestionsResponse | null;
  requireCurrentPassword?: boolean;
  onSaved?: (questions: SecurityQuestionsResponse) => void;
  onCancel?: () => void;
}

export function SecurityQuestionsSetup({
  mode = 'standalone',
  initialQuestions,
  requireCurrentPassword = false,
  onSaved,
  onCancel,
}: SecurityQuestionsSetupProps) {
  const navigate = useNavigate();
  const [securityQuestion1, setSecurityQuestion1] = useState(initialQuestions?.question1 || DEFAULT_SECURITY_QUESTION_1);
  const [securityQuestion2, setSecurityQuestion2] = useState(initialQuestions?.question2 || DEFAULT_SECURITY_QUESTION_2);
  const [customSecurityQuestion, setCustomSecurityQuestion] = useState(initialQuestions?.customQuestion || '');
  const [securityAnswer1, setSecurityAnswer1] = useState('');
  const [securityAnswer2, setSecurityAnswer2] = useState('');
  const [customSecurityAnswer, setCustomSecurityAnswer] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const finish = (questions: SecurityQuestionsResponse) => {
    onSaved?.(questions);
    if (mode === 'standalone') {
      navigate('/');
    }
  };

  const handleSkip = () => {
    navigate('/');
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (!securityQuestion1 || !securityQuestion2 || !customSecurityQuestion.trim()) {
      toast.error('Please choose two questions and enter a custom question');
      return;
    }

    if (securityQuestion1 === securityQuestion2) {
      toast.error('Please choose two different system questions');
      return;
    }

    if (!securityAnswer1.trim() || !securityAnswer2.trim() || !customSecurityAnswer.trim()) {
      toast.error('Please answer all three questions');
      return;
    }

    if (requireCurrentPassword && !currentPassword.trim()) {
      toast.error('Current password is required to update recovery questions');
      return;
    }

    setIsSaving(true);
    try {
      await apiService.setSecurityQuestions({
        currentPassword: requireCurrentPassword ? currentPassword : undefined,
        securityQuestion1,
        securityAnswer1,
        securityQuestion2,
        securityAnswer2,
        customSecurityQuestion: customSecurityQuestion.trim(),
        customSecurityAnswer,
      });

      const savedQuestions = {
        question1: securityQuestion1,
        question2: securityQuestion2,
        customQuestion: customSecurityQuestion.trim(),
      };
      toast.success('Recovery questions saved');
      finish(savedQuestions);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to save recovery questions');
    } finally {
      setIsSaving(false);
    }
  };

  const content = (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="flex items-start gap-3 pb-1">
        <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
          <ShieldCheck className="w-5 h-5" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-gray-900">Recovery questions</h2>
          <p className="text-sm text-gray-600 mt-1">
            These questions help verify your identity if you forget your password.
          </p>
        </div>
      </div>

      {requireCurrentPassword && (
        <div>
          <Label htmlFor="current-password">Current Password</Label>
          <Input
            id="current-password"
            type="password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            className="mt-2"
            disabled={isSaving}
          />
        </div>
      )}

      <div>
        <Label htmlFor="security-question-1">Security Question 1</Label>
        <select
          id="security-question-1"
          value={securityQuestion1}
          onChange={(event) => setSecurityQuestion1(event.target.value)}
          className="mt-2 w-full h-10 rounded-lg border border-gray-300 bg-white px-3 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
          disabled={isSaving}
        >
          <option value="">Choose a question</option>
          {SYSTEM_SECURITY_QUESTIONS.map((question) => (
            <option key={question} value={question}>{question}</option>
          ))}
        </select>
        <Input
          aria-label="Security answer 1"
          type="text"
          value={securityAnswer1}
          onChange={(event) => setSecurityAnswer1(event.target.value)}
          className="mt-2"
          disabled={isSaving}
        />
      </div>

      <div>
        <Label htmlFor="security-question-2">Security Question 2</Label>
        <select
          id="security-question-2"
          value={securityQuestion2}
          onChange={(event) => setSecurityQuestion2(event.target.value)}
          className="mt-2 w-full h-10 rounded-lg border border-gray-300 bg-white px-3 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
          disabled={isSaving}
        >
          <option value="">Choose a question</option>
          {SYSTEM_SECURITY_QUESTIONS.map((question) => (
            <option key={question} value={question}>{question}</option>
          ))}
        </select>
        <Input
          aria-label="Security answer 2"
          type="text"
          value={securityAnswer2}
          onChange={(event) => setSecurityAnswer2(event.target.value)}
          className="mt-2"
          disabled={isSaving}
        />
      </div>

      <div>
        <Label htmlFor="custom-security-question">Custom Security Question</Label>
        <Input
          id="custom-security-question"
          value={customSecurityQuestion}
          onChange={(event) => setCustomSecurityQuestion(event.target.value)}
          className="mt-2"
          maxLength={255}
          disabled={isSaving}
        />
        <Input
          aria-label="Custom security answer"
          type="text"
          value={customSecurityAnswer}
          onChange={(event) => setCustomSecurityAnswer(event.target.value)}
          className="mt-2"
          disabled={isSaving}
        />
      </div>

      <div className="flex gap-3 pt-2">
        {mode === 'standalone' ? (
          <Button type="button" variant="outline" onClick={handleSkip} className="flex-1 h-9 bg-white" disabled={isSaving}>
            Skip for now
          </Button>
        ) : (
          <Button type="button" variant="outline" onClick={onCancel} className="flex-1 h-9 bg-white" disabled={isSaving}>
            Cancel
          </Button>
        )}
        <Button type="submit" className="flex-1 h-9 bg-slate-950 text-white hover:bg-slate-900" disabled={isSaving}>
          {isSaving ? (
            <>
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              Saving...
            </>
          ) : (
            'Save Questions'
          )}
        </Button>
      </div>
    </form>
  );

  if (mode === 'embedded') {
    return content;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
      <div className="w-full max-w-xl bg-white rounded-xl border border-gray-200 shadow-md p-8">
        {content}
      </div>
    </div>
  );
}
